package com.service.domain.virtualsalary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;
import com.service.domain.virtualsalary.dto.response.ContractDetailResponse;
import com.service.domain.virtualsalary.dto.response.ContractListResponse;
import com.service.domain.virtualsalary.entity.Contract;
import com.service.domain.virtualsalary.entity.ContractSettlement;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.enumtype.TaxType;
import com.service.domain.virtualsalary.repository.ContractRepository;
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
class ContractServiceImplTest {

  @InjectMocks private ContractServiceImpl contractService;

  @Mock private ContractRepository contractRepository;

  @Test
  @DisplayName("계약 생성 시 3.3% 세금 공제 금액과 실수령액이 올바르게 계산된다")
  void createContract_calculatesDeductionCorrectly() {
    ContractCreateRequest request =
        createRequest(
            "카카오", new BigDecimal("5000000"), TaxType.BUSINESS, LocalDate.now().plusDays(10), null);
    given(contractRepository.save(any(Contract.class))).willAnswer(inv -> inv.getArgument(0));

    ContractCreateResponse response = contractService.createContract(1L, request);

    assertThat(response.getDeductedAmount()).isEqualByComparingTo(new BigDecimal("165000"));
    assertThat(response.getActualIncome()).isEqualByComparingTo(new BigDecimal("4835000"));
  }

  @Test
  @DisplayName("계약 생성 시 Contract와 ContractSettlement가 함께 저장된다")
  void createContract_savesContractWithSettlement() {
    ContractCreateRequest request =
        createRequest(
            "네이버", new BigDecimal("3000000"), TaxType.ETC, LocalDate.now().plusDays(5), "메모");
    given(contractRepository.save(any(Contract.class))).willAnswer(inv -> inv.getArgument(0));

    contractService.createContract(1L, request);

    then(contractRepository).should().save(any(Contract.class));
  }

  @Test
  @DisplayName("getContracts - date가 null이면 현재 월의 계약을 반환한다")
  void getContracts_returnsCurrentMonthContractsWhenDateIsNull() {
    LocalDate today = LocalDate.now();
    Contract contract = buildContract(1L, "클라이언트", new BigDecimal("3000000"), today);
    given(contractRepository.findAllByUserIdAndExpectedPaymentDateBetween(any(), any(), any()))
        .willReturn(List.of(contract));

    List<ContractListResponse> result = contractService.getContracts(1L, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getClientName()).isEqualTo("클라이언트");
  }

  @Test
  @DisplayName("getContracts - 특정 월을 지정하면 해당 월의 계약만 반환한다")
  void getContracts_returnsContractsInGivenMonth() {
    LocalDate targetDate = LocalDate.of(2026, 3, 15);
    Contract contract = buildContract(1L, "클라이언트A", new BigDecimal("2000000"), targetDate);
    given(contractRepository.findAllByUserIdAndExpectedPaymentDateBetween(any(), any(), any()))
        .willReturn(List.of(contract));

    List<ContractListResponse> result = contractService.getContracts(1L, targetDate);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getContracts - 해당 월에 계약이 없으면 빈 리스트를 반환한다")
  void getContracts_returnsEmptyListWhenNoContracts() {
    given(contractRepository.findAllByUserIdAndExpectedPaymentDateBetween(any(), any(), any()))
        .willReturn(Collections.emptyList());

    List<ContractListResponse> result = contractService.getContracts(1L, LocalDate.now());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getContractDetail - TBC 상태의 매칭을 우선 반환한다")
  void getContractDetail_returnsTbcMatchingFirst() {
    Contract contract =
        buildContract(1L, "카카오", new BigDecimal("5000000"), LocalDate.now().plusDays(10));
    PaymentMatching matched = buildMatching(1L, contract, MatchingStatus.MATCHED, MatchedBy.SYSTEM);
    PaymentMatching tbc = buildMatching(2L, contract, MatchingStatus.TBC, MatchedBy.SYSTEM);
    ReflectionTestUtils.setField(contract, "paymentMatchings", List.of(matched, tbc));
    given(contractRepository.findByContractIdAndUserId(1L, 1L)).willReturn(Optional.of(contract));

    ContractDetailResponse response = contractService.getContractDetail(1L, 1L);

    assertThat(response.getMatchingId()).isEqualTo(2L);
    assertThat(response.getMatchingStatus()).isEqualTo(MatchingStatus.TBC);
    assertThat(response.isCanComplete()).isTrue();
  }

  @Test
  @DisplayName("getContractDetail - 계약이 없으면 CONTRACT_001 예외가 발생한다")
  void getContractDetail_throwsExceptionWhenContractNotFound() {
    given(contractRepository.findByContractIdAndUserId(99L, 1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> contractService.getContractDetail(1L, 99L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.CONTRACT_001);
  }

  @Test
  @DisplayName("getContractDetail - 매칭이 없으면 canComplete는 false를 반환한다")
  void getContractDetail_returnsFalseCanCompleteWhenNoMatching() {
    Contract contract =
        buildContract(1L, "카카오", new BigDecimal("5000000"), LocalDate.now().plusDays(10));
    given(contractRepository.findByContractIdAndUserId(1L, 1L)).willReturn(Optional.of(contract));

    ContractDetailResponse response = contractService.getContractDetail(1L, 1L);

    assertThat(response.isCanComplete()).isFalse();
    assertThat(response.getMatchingId()).isNull();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private ContractCreateRequest createRequest(
      String clientName,
      BigDecimal contractAmount,
      TaxType taxType,
      LocalDate expectedPaymentDate,
      String memo) {
    ContractCreateRequest request = new ContractCreateRequest();
    ReflectionTestUtils.setField(request, "clientName", clientName);
    ReflectionTestUtils.setField(request, "contractAmount", contractAmount);
    ReflectionTestUtils.setField(request, "taxType", taxType);
    ReflectionTestUtils.setField(request, "expectedPaymentDate", expectedPaymentDate);
    ReflectionTestUtils.setField(request, "memo", memo);
    return request;
  }

  private Contract buildContract(
      Long contractId, String clientName, BigDecimal amount, LocalDate paymentDate) {
    BigDecimal taxRate = TaxType.BUSINESS.getRate();
    BigDecimal deducted = amount.multiply(taxRate);
    BigDecimal actualIncome = amount.subtract(deducted);

    Contract contract =
        Contract.builder()
            .userId(1L)
            .clientName(clientName)
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
}
