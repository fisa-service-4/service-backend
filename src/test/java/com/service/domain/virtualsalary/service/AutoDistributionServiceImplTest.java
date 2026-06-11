package com.service.domain.virtualsalary.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.entity.LinkedFinancialAccount;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserRepository;
import com.service.domain.virtualsalary.entity.Contract;
import com.service.domain.virtualsalary.entity.ContractSettlement;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.enumtype.TaxType;
import com.service.domain.virtualsalary.enumtype.VirtualSalaryCategory;
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.BankServerClient;
import com.service.global.client.TransactionServerClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AutoDistributionServiceImplTest {

  @InjectMocks private AutoDistributionServiceImpl distributionService;

  @Mock private PaymentMatchingRepository matchingRepository;

  @Mock private VirtualSalarySettingRepository settingRepository;

  @Mock private AccountMappingRepository accountMappingRepository;

  @Mock private UserRepository userRepository;

  @Mock private BankServerClient bankServerClient;

  @Mock private TransactionServerClient transactionServerClient;

  @Test
  @DisplayName("가상월급 설정이 없으면 distribute가 스킵된다")
  void distribute_skipsWhenNoVirtualSalarySetting() {
    given(settingRepository.findById(1L)).willReturn(Optional.empty());

    distributionService.distribute(1L, 1L);

    then(matchingRepository).should(never()).findById(anyLong());
    then(transactionServerClient)
        .should(never())
        .bankTransfer(any(), anyLong(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("INCOME 계좌가 미연결이면 distribute가 스킵된다")
  void distribute_skipsWhenNoIncomeAccount() {
    VirtualSalarySetting setting =
        buildSetting(
            new BigDecimal("3000000"),
            25,
            new BigDecimal("500000"),
            new BigDecimal("300000"),
            null,
            null);
    PaymentMatching matching = buildMatching(buildContract(new BigDecimal("4835000")));

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.empty());

    distributionService.distribute(1L, 1L);

    then(transactionServerClient)
        .should(never())
        .bankTransfer(any(), anyLong(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("INCOME 잔액 조회에 실패하면 distribute가 스킵된다")
  void distribute_skipsWhenIncomeBalanceFetchFails() {
    VirtualSalarySetting setting =
        buildSetting(
            new BigDecimal("3000000"),
            25,
            new BigDecimal("500000"),
            new BigDecimal("300000"),
            null,
            null);
    PaymentMatching matching = buildMatching(buildContract(new BigDecimal("4835000")));
    AccountMapping incomeMapping = buildAccountMapping(1001L, AccountMapping.MappingType.INCOME);

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(bankServerClient.getAccountBalance(1001L)).willThrow(new RuntimeException("서버 오류"));

    distributionService.distribute(1L, 1L);

    then(transactionServerClient)
        .should(never())
        .bankTransfer(any(), anyLong(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("우선순위가 SALARY→EMERGENCY→INVESTMENT일 때 순서대로 분배된다")
  void distribute_executesTransfersWithPriorityOrder() {
    // actualIncome = 4,835,000
    // priority: SALARY(3,000,000) → EMERGENCY(500,000) → INVESTMENT(300,000)
    // 잔여: 4,835,000 - 3,000,000 = 1,835,000 → emergency 500,000 → investment 300,000
    VirtualSalarySetting setting =
        buildSetting(
            new BigDecimal("3000000"),
            25,
            new BigDecimal("500000"),
            new BigDecimal("300000"),
            null,
            List.of(
                VirtualSalaryCategory.SALARY,
                VirtualSalaryCategory.EMERGENCY,
                VirtualSalaryCategory.INVESTMENT));
    PaymentMatching matching = buildMatching(buildContract(new BigDecimal("4835000")));
    AccountMapping incomeMapping = buildAccountMapping(1001L, AccountMapping.MappingType.INCOME);
    AccountMapping emergencyMapping =
        buildAccountMapping(2001L, AccountMapping.MappingType.EMERGENCY);
    AccountMapping investmentMapping = buildAccountMapping(3001L, AccountMapping.MappingType.STOCK);
    BankServerClient.BankAccountDetailData emergencyDetail = buildAccountDetail("2001-111", "088");
    BankServerClient.StockAccountItem investmentDetail = buildStockAccountItem("3001-222", "088");
    User user = User.builder().firebaseUid("firebase-uid-1").build();
    TransactionServerClient.BankTransferResult transferResult = buildTransferResult(999L);

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(bankServerClient.getAccountBalance(1001L)).willReturn(new BigDecimal("4835000"));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.EMERGENCY))
        .willReturn(Optional.of(emergencyMapping));
    given(accountMappingRepository.findByUserIdAndMappingType(1L, AccountMapping.MappingType.STOCK))
        .willReturn(Optional.of(investmentMapping));
    given(bankServerClient.getBankAccountDetail(2001L)).willReturn(emergencyDetail);
    given(userRepository.findById(1L)).willReturn(Optional.of(user));
    given(bankServerClient.getStockAccountDetail("firebase-uid-1", 3001L))
        .willReturn(investmentDetail);
    given(transactionServerClient.bankTransfer(any(), anyLong(), any(), any(), any(), any()))
        .willReturn(transferResult);

    distributionService.distribute(1L, 1L);

    // emergency와 investment 이체 각 1회 호출 검증
    then(transactionServerClient)
        .should(org.mockito.Mockito.times(2))
        .bankTransfer(any(), anyLong(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("우선순위가 없으면 emergency → investment 순서로 기본 분배된다")
  void distribute_usesDefaultOrderWhenNoPriorityOrder() {
    // priority order = null → emergency(500,000), investment(300,000) 순 기본 처리
    VirtualSalarySetting setting =
        buildSetting(
            new BigDecimal("3000000"),
            25,
            new BigDecimal("500000"),
            new BigDecimal("300000"),
            null,
            null);
    PaymentMatching matching = buildMatching(buildContract(new BigDecimal("4835000")));
    AccountMapping incomeMapping = buildAccountMapping(1001L, AccountMapping.MappingType.INCOME);
    AccountMapping emergencyMapping =
        buildAccountMapping(2001L, AccountMapping.MappingType.EMERGENCY);
    AccountMapping investmentMapping = buildAccountMapping(3001L, AccountMapping.MappingType.STOCK);
    BankServerClient.BankAccountDetailData emergencyDetail = buildAccountDetail("2001-111", "088");
    BankServerClient.StockAccountItem investmentDetail = buildStockAccountItem("3001-222", "088");
    User user = User.builder().firebaseUid("firebase-uid-1").build();
    TransactionServerClient.BankTransferResult transferResult = buildTransferResult(999L);

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(bankServerClient.getAccountBalance(1001L)).willReturn(new BigDecimal("4835000"));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.EMERGENCY))
        .willReturn(Optional.of(emergencyMapping));
    given(accountMappingRepository.findByUserIdAndMappingType(1L, AccountMapping.MappingType.STOCK))
        .willReturn(Optional.of(investmentMapping));
    given(bankServerClient.getBankAccountDetail(2001L)).willReturn(emergencyDetail);
    given(userRepository.findById(1L)).willReturn(Optional.of(user));
    given(bankServerClient.getStockAccountDetail("firebase-uid-1", 3001L))
        .willReturn(investmentDetail);
    given(transactionServerClient.bankTransfer(any(), anyLong(), any(), any(), any(), any()))
        .willReturn(transferResult);

    distributionService.distribute(1L, 1L);

    then(transactionServerClient)
        .should(org.mockito.Mockito.times(2))
        .bankTransfer(any(), anyLong(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("잔액이 이체 합계보다 부족하면 우선순위에 따라 이체 금액이 cap된다")
  void distribute_capsTransferAmountWhenBalanceInsufficient() {
    // actualIncome = 1,100,000 / emergency = 500,000 / investment = 600,000
    // balance = 800,000 < totalToTransfer(1,100,000) → capByBalance 실행
    // expected: emergency = 500,000, investment = min(600,000, 800,000-500,000=300,000) = 300,000
    VirtualSalarySetting setting =
        buildSetting(
            new BigDecimal("0"),
            25,
            new BigDecimal("500000"),
            new BigDecimal("600000"),
            null,
            List.of(VirtualSalaryCategory.EMERGENCY, VirtualSalaryCategory.INVESTMENT));
    PaymentMatching matching = buildMatching(buildContract(new BigDecimal("1100000")));
    AccountMapping incomeMapping = buildAccountMapping(1001L, AccountMapping.MappingType.INCOME);
    AccountMapping emergencyMapping =
        buildAccountMapping(2001L, AccountMapping.MappingType.EMERGENCY);
    AccountMapping investmentMapping = buildAccountMapping(3001L, AccountMapping.MappingType.STOCK);
    BankServerClient.BankAccountDetailData emergencyDetail = buildAccountDetail("2001-111", "088");
    BankServerClient.StockAccountItem investmentDetail = buildStockAccountItem("3001-222", "088");
    User user = User.builder().firebaseUid("firebase-uid-1").build();
    TransactionServerClient.BankTransferResult transferResult = buildTransferResult(999L);

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(bankServerClient.getAccountBalance(1001L)).willReturn(new BigDecimal("800000"));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.EMERGENCY))
        .willReturn(Optional.of(emergencyMapping));
    given(accountMappingRepository.findByUserIdAndMappingType(1L, AccountMapping.MappingType.STOCK))
        .willReturn(Optional.of(investmentMapping));
    given(bankServerClient.getBankAccountDetail(2001L)).willReturn(emergencyDetail);
    given(userRepository.findById(1L)).willReturn(Optional.of(user));
    given(bankServerClient.getStockAccountDetail("firebase-uid-1", 3001L))
        .willReturn(investmentDetail);
    given(transactionServerClient.bankTransfer(any(), anyLong(), any(), any(), any(), any()))
        .willReturn(transferResult);

    distributionService.distribute(1L, 1L);

    then(transactionServerClient)
        .should(org.mockito.Mockito.times(2))
        .bankTransfer(any(), anyLong(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("비상금 목표 금액이 설정되어 있으면 현재 잔액을 고려하여 이체 금액이 cap된다")
  void distribute_capsEmergencyAmountWithTargetAmount() {
    // emergencyTargetAmount = 5,000,000 / currentEmergencyBalance = 4,800,000
    // remaining = 200,000 → actualEmergency = min(500,000, 200,000) = 200,000
    VirtualSalarySetting setting =
        buildSetting(
            new BigDecimal("3000000"),
            25,
            new BigDecimal("500000"),
            BigDecimal.ZERO,
            new BigDecimal("5000000"),
            List.of(VirtualSalaryCategory.EMERGENCY));
    PaymentMatching matching = buildMatching(buildContract(new BigDecimal("4835000")));
    AccountMapping incomeMapping = buildAccountMapping(1001L, AccountMapping.MappingType.INCOME);
    AccountMapping emergencyMapping =
        buildAccountMapping(2001L, AccountMapping.MappingType.EMERGENCY);
    BankServerClient.BankAccountDetailData emergencyDetail = buildAccountDetail("2001-111", "088");

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(bankServerClient.getAccountBalance(1001L)).willReturn(new BigDecimal("4835000"));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.EMERGENCY))
        .willReturn(Optional.of(emergencyMapping));
    given(bankServerClient.getAccountBalance(2001L)).willReturn(new BigDecimal("4800000"));
    given(bankServerClient.getBankAccountDetail(2001L)).willReturn(emergencyDetail);

    distributionService.distribute(1L, 1L);

    then(transactionServerClient)
        .should()
        .bankTransfer(
            any(), eq(1001L), eq("088"), eq("2001-111"), eq(new BigDecimal("200000")), eq("AI"));
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private VirtualSalarySetting buildSetting(
      BigDecimal targetSalary,
      int payday,
      BigDecimal emergencyAmount,
      BigDecimal investmentAmount,
      BigDecimal emergencyTargetAmount,
      List<VirtualSalaryCategory> priorityOrder) {
    return VirtualSalarySetting.builder()
        .userId(1L)
        .targetSalary(targetSalary)
        .payday(payday)
        .emergencyAmount(emergencyAmount)
        .investmentAmount(investmentAmount)
        .emergencyTargetAmount(emergencyTargetAmount)
        .priorityOrder(priorityOrder)
        .build();
  }

  private Contract buildContract(BigDecimal actualIncome) {
    BigDecimal contractAmount =
        actualIncome.divide(new BigDecimal("0.967"), 0, java.math.RoundingMode.UP);
    BigDecimal taxRate = TaxType.BUSINESS.getRate();
    BigDecimal deducted = contractAmount.subtract(actualIncome);

    Contract contract =
        Contract.builder()
            .userId(1L)
            .clientName("테스트")
            .contractAmount(contractAmount)
            .taxType(TaxType.BUSINESS)
            .taxRate(taxRate)
            .expectedPaymentDate(LocalDate.now().plusDays(10))
            .contractStatus(ContractStatus.PENDING)
            .build();
    ReflectionTestUtils.setField(contract, "contractId", 1L);

    ContractSettlement settlement =
        ContractSettlement.builder()
            .taxRate(taxRate)
            .deductedAmount(deducted)
            .actualIncome(actualIncome)
            .build();
    settlement.assignContract(contract);
    ReflectionTestUtils.setField(contract, "settlement", settlement);
    return contract;
  }

  private PaymentMatching buildMatching(Contract contract) {
    PaymentMatching matching =
        PaymentMatching.builder()
            .contract(contract)
            .matchingStatus(MatchingStatus.MATCHED)
            .matchedBy(MatchedBy.SYSTEM)
            .transactionAmount(contract.getSettlement().getActualIncome())
            .build();
    ReflectionTestUtils.setField(matching, "matchingId", 1L);
    return matching;
  }

  private AccountMapping buildAccountMapping(
      Long externalAccountId, AccountMapping.MappingType type) {
    LinkedFinancialAccount linkedAccount =
        LinkedFinancialAccount.builder()
            .institutionType(LinkedFinancialAccount.InstitutionType.BANK)
            .institutionCode("088")
            .externalAccountId(externalAccountId)
            .accountMasking("110-***-***")
            .build();
    return AccountMapping.builder()
        .userId(1L)
        .linkedFinancialAccount(linkedAccount)
        .mappingType(type)
        .build();
  }

  private BankServerClient.BankAccountDetailData buildAccountDetail(
      String accountNumber, String bankCode) {
    BankServerClient.BankAccountDetailData detail = new BankServerClient.BankAccountDetailData();
    ReflectionTestUtils.setField(detail, "accountNumber", accountNumber);
    ReflectionTestUtils.setField(detail, "bankCode", bankCode);
    return detail;
  }

  private BankServerClient.StockAccountItem buildStockAccountItem(
      String accountNumber, String bankCode) {
    BankServerClient.StockAccountItem item = new BankServerClient.StockAccountItem();
    ReflectionTestUtils.setField(item, "accountNumber", accountNumber);
    ReflectionTestUtils.setField(item, "bankCode", bankCode);
    return item;
  }

  private TransactionServerClient.BankTransferResult buildTransferResult(Long transferId) {
    TransactionServerClient.BankTransferResult result =
        new TransactionServerClient.BankTransferResult();
    ReflectionTestUtils.setField(result, "transferId", transferId);
    return result;
  }
}
