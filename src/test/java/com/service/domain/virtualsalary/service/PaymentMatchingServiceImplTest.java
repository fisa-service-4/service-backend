package com.service.domain.virtualsalary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.entity.LinkedFinancialAccount;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.dto.request.ManualMatchingRequest;
import com.service.domain.virtualsalary.dto.response.ManualMatchingResponse;
import com.service.domain.virtualsalary.entity.Contract;
import com.service.domain.virtualsalary.entity.ContractSettlement;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.enumtype.TaxType;
import com.service.domain.virtualsalary.repository.ContractRepository;
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentMatchingServiceImplTest {

  @InjectMocks private PaymentMatchingServiceImpl matchingService;

  @Mock private PaymentMatchingRepository matchingRepository;

  @Mock private ContractRepository contractRepository;

  @Mock private AutoDistributionService autoDistributionService;

  @Mock private AccountMappingRepository accountMappingRepository;

  @Mock private TransactionServerClient transactionServerClient;

  // ─── manualMatch ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("manualMatch - matchedBy가 USER가 아니면 MATCHING_003 예외가 발생한다")
  void manualMatch_throwsWhenMatchedByIsNotUser() {
    ManualMatchingRequest request = buildMatchingRequest(9001L, "SYSTEM");

    assertThatThrownBy(() -> matchingService.manualMatch(1L, 1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.MATCHING_003);
  }

  @Test
  @DisplayName("manualMatch - 매칭 정보가 없으면 MATCHING_001 예외가 발생한다")
  void manualMatch_throwsWhenMatchingNotFound() {
    ManualMatchingRequest request = buildMatchingRequest(9001L, "USER");
    given(matchingRepository.findByMatchingIdAndContract_UserId(99L, 1L))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> matchingService.manualMatch(1L, 99L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.MATCHING_001);
  }

  @Test
  @DisplayName("manualMatch - 이미 MATCHED 상태이면 MATCHING_002 예외가 발생한다")
  void manualMatch_throwsWhenAlreadyMatched() {
    ManualMatchingRequest request = buildMatchingRequest(9001L, "USER");
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(10));
    PaymentMatching matching =
        buildMatching(1L, contract, MatchingStatus.MATCHED, MatchedBy.SYSTEM);
    given(matchingRepository.findByMatchingIdAndContract_UserId(1L, 1L))
        .willReturn(Optional.of(matching));

    assertThatThrownBy(() -> matchingService.manualMatch(1L, 1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.MATCHING_002);
  }

  @Test
  @DisplayName("manualMatch - TBC 상태에서 수동 매칭이 성공한다")
  void manualMatch_successWhenTbc() {
    ManualMatchingRequest request = buildMatchingRequest(9001L, "USER");
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(10));
    PaymentMatching matching = buildMatching(1L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);
    given(matchingRepository.findByMatchingIdAndContract_UserId(1L, 1L))
        .willReturn(Optional.of(matching));

    ManualMatchingResponse response = matchingService.manualMatch(1L, 1L, request);

    assertThat(response.getMatchingId()).isEqualTo(1L);
    assertThat(response.getMatchingStatus()).isEqualTo(MatchingStatus.MATCHED);
    assertThat(response.getMatchedBy()).isEqualTo(MatchedBy.USER);
    assertThat(contract.getContractStatus()).isEqualTo(ContractStatus.PAID);
  }

  // ─── processDeposit ───────────────────────────────────────────────────────

  @Test
  @DisplayName("processDeposit - INCOME 계좌가 미연결이면 처리를 스킵한다")
  void processDeposit_skipsWhenNoIncomeMapping() {
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.empty());

    matchingService.processDeposit(1L, 1001L, 9001L, new BigDecimal("4835000"));

    then(matchingRepository).should(never()).findTbcByUserId(1L);
  }

  @Test
  @DisplayName("processDeposit - 입금 계좌 ID가 INCOME 계좌와 다르면 스킵한다")
  void processDeposit_skipsWhenAccountIdMismatch() {
    AccountMapping incomeMapping = buildAccountMapping(2000L);
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));

    matchingService.processDeposit(1L, 9999L, 9001L, new BigDecimal("4835000"));

    then(matchingRepository).should(never()).findTbcByUserId(1L);
  }

  @Test
  @DisplayName("processDeposit - TBC 매칭이 없으면 처리를 스킵한다")
  void processDeposit_skipsWhenNoTbcMatchings() {
    AccountMapping incomeMapping = buildAccountMapping(1001L);
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(matchingRepository.findTbcByUserId(1L)).willReturn(Collections.emptyList());

    matchingService.processDeposit(1L, 1001L, 9001L, new BigDecimal("4835000"));

    then(autoDistributionService).should(never()).distribute(1L, 1L);
  }

  @Test
  @DisplayName("processDeposit - 입금액이 허용 범위(97~103%) 내이면 자동 매칭 처리된다")
  void processDeposit_autoMatchesWhenAmountInRange() {
    // actualIncome = 4,835,000 / depositAmount = 4,835,000 (100% 정확)
    BigDecimal actualIncome = new BigDecimal("4835000");
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(5));
    PaymentMatching tbc = buildMatching(1L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);
    AccountMapping incomeMapping = buildAccountMapping(1001L);

    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(matchingRepository.findTbcByUserId(1L)).willReturn(List.of(tbc));

    matchingService.processDeposit(1L, 1001L, 9001L, actualIncome);

    assertThat(tbc.getMatchingStatus()).isEqualTo(MatchingStatus.MATCHED);
    assertThat(tbc.getMatchedBy()).isEqualTo(MatchedBy.SYSTEM);
    assertThat(contract.getContractStatus()).isEqualTo(ContractStatus.PAID);
    then(autoDistributionService).should().distribute(1L, 1L);
  }

  @Test
  @DisplayName("processDeposit - 입금액이 허용 범위 밖이면 TBC 상태를 유지하고 입금 정보만 연결한다")
  void processDeposit_keepsTbcWhenAmountOutOfRange() {
    // actualIncome = 4,835,000 / depositAmount = 1,000 (완전히 범위 벗어남)
    BigDecimal actualIncome = new BigDecimal("4835000");
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(5));
    PaymentMatching tbc = buildMatching(1L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);
    AccountMapping incomeMapping = buildAccountMapping(1001L);

    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(incomeMapping));
    given(matchingRepository.findTbcByUserId(1L)).willReturn(List.of(tbc));

    matchingService.processDeposit(1L, 1001L, 9001L, new BigDecimal("1000"));

    assertThat(tbc.getMatchingStatus()).isEqualTo(MatchingStatus.TBC);
    assertThat(tbc.getBankTransactionId()).isEqualTo(9001L);
    then(autoDistributionService).should(never()).distribute(1L, 1L);
  }

  // ─── expireOverdueTbcMatchings ────────────────────────────────────────────

  @Test
  @DisplayName("expireOverdueTbcMatchings - 기한 초과 TBC 매칭을 FAILED로 처리하고 계약을 DELAYED로 변경한다")
  void expireOverdueTbcMatchings_marksExpiredAsFailedAndDelayed() {
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().minusDays(1));
    PaymentMatching tbc = buildMatching(1L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);
    given(matchingRepository.findExpiredTbc(any(LocalDate.class))).willReturn(List.of(tbc));

    matchingService.expireOverdueTbcMatchings();

    assertThat(tbc.getMatchingStatus()).isEqualTo(MatchingStatus.FAILED);
    assertThat(contract.getContractStatus()).isEqualTo(ContractStatus.DELAYED);
  }

  // ─── pollAndMatchForUser ──────────────────────────────────────────────────

  @Test
  @DisplayName("pollAndMatchForUser - INCOME 계좌 미연결이면 거래내역 조회를 하지 않는다")
  void pollAndMatchForUser_skipsWhenNoIncomeMapping() {
    given(
            accountMappingRepository.findByUserIdAndMappingTypeFetch(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.empty());

    matchingService.pollAndMatchForUser(1L);

    then(transactionServerClient)
        .should(never())
        .getBankTransactions(anyLong(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  @DisplayName("pollAndMatchForUser - 거래내역 조회 예외 발생 시 매칭 처리를 스킵한다")
  void pollAndMatchForUser_skipsWhenTransactionClientThrows() {
    given(
            accountMappingRepository.findByUserIdAndMappingTypeFetch(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(buildAccountMapping(1001L)));
    given(
            transactionServerClient.getBankTransactions(
                anyLong(), any(), any(), any(), anyInt(), anyInt()))
        .willThrow(new RuntimeException("connection failed"));

    matchingService.pollAndMatchForUser(1L);

    then(matchingRepository).should(never()).findTbcByUserIdFetch(1L);
  }

  @Test
  @DisplayName("pollAndMatchForUser - 거래내역이 없으면 매칭 처리를 하지 않는다")
  void pollAndMatchForUser_skipsWhenNoTransactions() {
    given(
            accountMappingRepository.findByUserIdAndMappingTypeFetch(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(buildAccountMapping(1001L)));
    given(
            transactionServerClient.getBankTransactions(
                anyLong(), any(), any(), any(), anyInt(), anyInt()))
        .willReturn(null);

    matchingService.pollAndMatchForUser(1L);

    then(matchingRepository).should(never()).findTbcByUserIdFetch(anyLong());
  }

  @Test
  @DisplayName("pollAndMatchForUser - ±3% 범위 내 입금이면 자동 매칭 및 분배가 처리된다")
  void pollAndMatchForUser_autoMatchesWhenDepositInRange() {
    // contractAmount=5,000,000 → actualIncome=4,835,000
    // deposit=4,835,000 → lower=4,690,450 ~ upper=5,150,000 범위 내
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(5));
    PaymentMatching tbc = buildMatching(10L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);

    given(
            accountMappingRepository.findByUserIdAndMappingTypeFetch(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(buildAccountMapping(1001L)));
    given(
            transactionServerClient.getBankTransactions(
                anyLong(), any(), any(), any(), anyInt(), anyInt()))
        .willReturn(
            buildPageData(
                List.of(buildTxItem(9001L, "DEPOSIT", "SUCCESS", new BigDecimal("4835000")))));
    given(matchingRepository.findTbcByUserIdFetch(1L)).willReturn(List.of(tbc));
    given(autoDistributionService.distribute(1L, 10L)).willReturn(true);

    matchingService.pollAndMatchForUser(1L);

    assertThat(tbc.getMatchingStatus()).isEqualTo(MatchingStatus.MATCHED);
    assertThat(tbc.getMatchedBy()).isEqualTo(MatchedBy.SYSTEM);
    assertThat(tbc.getBankTransactionId()).isEqualTo(9001L);
    assertThat(contract.getContractStatus()).isEqualTo(ContractStatus.PAID);
    assertThat(tbc.isDistributedYn()).isTrue();
  }

  @Test
  @DisplayName("pollAndMatchForUser - 범위 밖 입금이면 TBC를 유지하고 linkDeposit으로 연결한다")
  void pollAndMatchForUser_linkDepositsWhenOutOfRange() {
    // actualIncome=4,835,000 / deposit=1,000 → 범위 밖 → linkDeposit
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(5));
    PaymentMatching tbc = buildMatching(10L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);

    given(
            accountMappingRepository.findByUserIdAndMappingTypeFetch(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(buildAccountMapping(1001L)));
    given(
            transactionServerClient.getBankTransactions(
                anyLong(), any(), any(), any(), anyInt(), anyInt()))
        .willReturn(
            buildPageData(
                List.of(buildTxItem(9001L, "DEPOSIT", "SUCCESS", new BigDecimal("1000")))));
    given(matchingRepository.findTbcByUserIdFetch(1L)).willReturn(List.of(tbc));

    matchingService.pollAndMatchForUser(1L);

    assertThat(tbc.getMatchingStatus()).isEqualTo(MatchingStatus.TBC);
    assertThat(tbc.getBankTransactionId()).isEqualTo(9001L);
    then(autoDistributionService).should(never()).distribute(anyLong(), anyLong());
  }

  @Test
  @DisplayName("pollAndMatchForUser - 이미 MATCHED된 txId는 가용 목록에서 제외하고 스킵한다")
  void pollAndMatchForUser_excludesAlreadyMatchedTxIds() {
    given(
            accountMappingRepository.findByUserIdAndMappingTypeFetch(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(buildAccountMapping(1001L)));
    given(
            transactionServerClient.getBankTransactions(
                anyLong(), any(), any(), any(), anyInt(), anyInt()))
        .willReturn(
            buildPageData(
                List.of(buildTxItem(9001L, "DEPOSIT", "SUCCESS", new BigDecimal("4835000")))));
    // 9001L은 이미 MATCHED된 txId → 가용 입금 없음 → TBC 조회 전에 리턴
    given(matchingRepository.findMatchedTransactionIdsByUserId(1L)).willReturn(Set.of(9001L));

    matchingService.pollAndMatchForUser(1L);

    then(matchingRepository).should(never()).findTbcByUserIdFetch(anyLong());
    then(autoDistributionService).should(never()).distribute(anyLong(), anyLong());
  }

  @Test
  @DisplayName("pollAndMatchForUser - 복수 TBC와 입금이 있을 때 차액 최소 원칙으로 그리디 매칭한다")
  void pollAndMatchForUser_greedyMatchesOptimalPairs() {
    // contract1: actualIncome=4,835,000 / contract2: actualIncome=2,901,000
    // deposit1=4,835,000 → contract1과 정확히 일치 (diff=0)
    // deposit2=3,000,000 → contract2 범위 내 [2,813,970~3,090,000] (diff=99,000)
    Contract contract1 = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(5));
    Contract contract2 = buildContract(2L, new BigDecimal("3000000"), LocalDate.now().plusDays(10));
    PaymentMatching tbc1 = buildMatching(10L, contract1, MatchingStatus.TBC, MatchedBy.SYSTEM);
    PaymentMatching tbc2 = buildMatching(11L, contract2, MatchingStatus.TBC, MatchedBy.SYSTEM);

    given(
            accountMappingRepository.findByUserIdAndMappingTypeFetch(
                1L, AccountMapping.MappingType.INCOME))
        .willReturn(Optional.of(buildAccountMapping(1001L)));
    given(
            transactionServerClient.getBankTransactions(
                anyLong(), any(), any(), any(), anyInt(), anyInt()))
        .willReturn(
            buildPageData(
                List.of(
                    buildTxItem(9001L, "DEPOSIT", "SUCCESS", new BigDecimal("4835000")),
                    buildTxItem(9002L, "TRANSFER_IN", "SUCCESS", new BigDecimal("3000000")))));
    given(matchingRepository.findTbcByUserIdFetch(1L)).willReturn(List.of(tbc1, tbc2));
    given(autoDistributionService.distribute(1L, 10L)).willReturn(true);
    given(autoDistributionService.distribute(1L, 11L)).willReturn(true);

    matchingService.pollAndMatchForUser(1L);

    assertThat(tbc1.getMatchingStatus()).isEqualTo(MatchingStatus.MATCHED);
    assertThat(tbc1.getBankTransactionId()).isEqualTo(9001L);
    assertThat(tbc2.getMatchingStatus()).isEqualTo(MatchingStatus.MATCHED);
    assertThat(tbc2.getBankTransactionId()).isEqualTo(9002L);
    assertThat(tbc1.isDistributedYn()).isTrue();
    assertThat(tbc2.isDistributedYn()).isTrue();
  }

  // ─── retryPendingDistributions ────────────────────────────────────────────

  @Test
  @DisplayName("retryPendingDistributions - 미완료 분배가 없으면 distribute를 호출하지 않는다")
  void retryPendingDistributions_skipsWhenNoPending() {
    given(matchingRepository.findMatchedWithoutDistributionFetch()).willReturn(Collections.emptyList());

    matchingService.retryPendingDistributions();

    then(autoDistributionService).should(never()).distribute(anyLong(), anyLong());
  }

  @Test
  @DisplayName("retryPendingDistributions - 분배 성공 시 distributedYn이 true로 변경된다")
  void retryPendingDistributions_markDistributedOnSuccess() {
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(5));
    PaymentMatching matching =
        buildMatching(1L, contract, MatchingStatus.MATCHED, MatchedBy.SYSTEM);
    given(matchingRepository.findMatchedWithoutDistributionFetch()).willReturn(List.of(matching));
    given(autoDistributionService.distribute(1L, 1L)).willReturn(true);

    matchingService.retryPendingDistributions();

    assertThat(matching.isDistributedYn()).isTrue();
  }

  @Test
  @DisplayName("retryPendingDistributions - 분배가 false를 반환하면 distributedYn은 false를 유지한다")
  void retryPendingDistributions_doesNotMarkWhenDistributionReturnsFalse() {
    Contract contract = buildContract(1L, new BigDecimal("5000000"), LocalDate.now().plusDays(5));
    PaymentMatching matching =
        buildMatching(1L, contract, MatchingStatus.MATCHED, MatchedBy.SYSTEM);
    given(matchingRepository.findMatchedWithoutDistributionFetch()).willReturn(List.of(matching));
    given(autoDistributionService.distribute(1L, 1L)).willReturn(false);

    matchingService.retryPendingDistributions();

    assertThat(matching.isDistributedYn()).isFalse();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private ManualMatchingRequest buildMatchingRequest(Long bankTransactionId, String matchedBy) {
    ManualMatchingRequest request = new ManualMatchingRequest();
    ReflectionTestUtils.setField(request, "bankTransactionId", bankTransactionId);
    ReflectionTestUtils.setField(request, "matchedBy", matchedBy);
    return request;
  }

  private Contract buildContract(Long contractId, BigDecimal amount, LocalDate paymentDate) {
    BigDecimal taxRate = TaxType.BUSINESS.getRate();
    BigDecimal deducted = amount.multiply(taxRate);
    BigDecimal actualIncome = amount.subtract(deducted);

    Contract contract =
        Contract.builder()
            .userId(1L)
            .clientName("테스트 클라이언트")
            .contractAmount(amount)
            .taxType(TaxType.BUSINESS)
            .taxRate(taxRate)
            .expectedPaymentDate(paymentDate)
            .contractStatus(ContractStatus.PENDING)
            .build();
    ReflectionTestUtils.setField(contract, "contractId", contractId);

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

  private PaymentMatching buildMatching(
      Long matchingId, Contract contract, MatchingStatus status, MatchedBy matchedBy) {
    PaymentMatching matching =
        PaymentMatching.builder()
            .contract(contract)
            .matchingStatus(status)
            .matchedBy(matchedBy)
            .build();
    ReflectionTestUtils.setField(matching, "matchingId", matchingId);
    return matching;
  }

  private AccountMapping buildAccountMapping(Long externalAccountId) {
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
        .mappingType(AccountMapping.MappingType.INCOME)
        .build();
  }

  private TransactionServerClient.BankTransactionItem buildTxItem(
      Long transactionId, String type, String status, BigDecimal amount) {
    TransactionServerClient.BankTransactionItem item =
        new TransactionServerClient.BankTransactionItem();
    ReflectionTestUtils.setField(item, "transactionId", transactionId);
    ReflectionTestUtils.setField(item, "transactionType", type);
    ReflectionTestUtils.setField(item, "transactionStatus", status);
    ReflectionTestUtils.setField(item, "amount", amount);
    return item;
  }

  private TransactionServerClient.TxPageData<TransactionServerClient.BankTransactionItem>
      buildPageData(List<TransactionServerClient.BankTransactionItem> items) {
    TransactionServerClient.TxPageData<TransactionServerClient.BankTransactionItem> page =
        new TransactionServerClient.TxPageData<>();
    ReflectionTestUtils.setField(page, "content", items);
    return page;
  }
}
