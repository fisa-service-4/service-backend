package com.service.domain.virtualsalary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
class PaymentMatchingServiceImplTest {

  @InjectMocks private PaymentMatchingServiceImpl matchingService;

  @Mock private PaymentMatchingRepository matchingRepository;

  @Mock private AutoDistributionService autoDistributionService;

  @Mock private AccountMappingRepository accountMappingRepository;

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
    Contract contract = buildContract(new BigDecimal("5000000"), LocalDate.now().plusDays(10));
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
    Contract contract = buildContract(new BigDecimal("5000000"), LocalDate.now().plusDays(10));
    PaymentMatching matching = buildMatching(1L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);
    given(matchingRepository.findByMatchingIdAndContract_UserId(1L, 1L))
        .willReturn(Optional.of(matching));

    ManualMatchingResponse response = matchingService.manualMatch(1L, 1L, request);

    assertThat(response.getMatchingId()).isEqualTo(1L);
    assertThat(response.getMatchingStatus()).isEqualTo(MatchingStatus.MATCHED);
    assertThat(response.getMatchedBy()).isEqualTo(MatchedBy.USER);
    assertThat(contract.getContractStatus()).isEqualTo(ContractStatus.PAID);
  }

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
    Contract contract = buildContract(new BigDecimal("5000000"), LocalDate.now().plusDays(5));
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
    Contract contract = buildContract(new BigDecimal("5000000"), LocalDate.now().plusDays(5));
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

  @Test
  @DisplayName("expireOverdueTbcMatchings - 기한 초과 TBC 매칭을 FAILED로 처리하고 계약을 DELAYED로 변경한다")
  void expireOverdueTbcMatchings_marksExpiredAsFailedAndDelayed() {
    Contract contract = buildContract(new BigDecimal("5000000"), LocalDate.now().minusDays(1));
    PaymentMatching tbc = buildMatching(1L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);
    given(matchingRepository.findExpiredTbc(any(LocalDate.class))).willReturn(List.of(tbc));

    matchingService.expireOverdueTbcMatchings();

    assertThat(tbc.getMatchingStatus()).isEqualTo(MatchingStatus.FAILED);
    assertThat(contract.getContractStatus()).isEqualTo(ContractStatus.DELAYED);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private ManualMatchingRequest buildMatchingRequest(Long bankTransactionId, String matchedBy) {
    ManualMatchingRequest request = new ManualMatchingRequest();
    ReflectionTestUtils.setField(request, "bankTransactionId", bankTransactionId);
    ReflectionTestUtils.setField(request, "matchedBy", matchedBy);
    return request;
  }

  private Contract buildContract(BigDecimal amount, LocalDate paymentDate) {
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
}
