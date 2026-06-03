package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.dto.request.ManualMatchingRequest;
import com.service.domain.virtualsalary.dto.response.ManualMatchingResponse;
import com.service.domain.virtualsalary.dto.response.PaymentMatchingResponse;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentMatchingServiceImpl implements PaymentMatchingService {

  private static final BigDecimal MATCH_THRESHOLD_RATE = new BigDecimal("0.97");

  private final PaymentMatchingRepository paymentMatchingRepository;
  private final AutoDistributionService autoDistributionService;
  private final AccountMappingRepository accountMappingRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PaymentMatchingResponse> getMatchings(
      Long userId, Long contractId, MatchingStatus matchingStatus, LocalDate from, LocalDate to) {

    LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
    LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

    return paymentMatchingRepository.findAllByFilters(userId).stream()
        .filter(pm -> contractId == null || pm.getContract().getContractId().equals(contractId))
        .filter(pm -> matchingStatus == null || pm.getMatchingStatus() == matchingStatus)
        .filter(
            pm ->
                fromDateTime == null
                    || pm.getMatchedAt() == null
                    || !pm.getMatchedAt().isBefore(fromDateTime))
        .filter(
            pm ->
                toDateTime == null
                    || pm.getMatchedAt() == null
                    || !pm.getMatchedAt().isAfter(toDateTime))
        .map(this::toResponse)
        .toList();
  }

  @Override
  public ManualMatchingResponse manualMatch(
      Long userId, Long matchingId, ManualMatchingRequest request) {

    if (!MatchedBy.USER.name().equals(request.getMatchedBy())) {
      throw new BusinessException(ErrorCode.MATCHING_003);
    }

    PaymentMatching matching =
        paymentMatchingRepository
            .findByMatchingIdAndContract_UserId(matchingId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MATCHING_001));

    // TBC 상태인 경우에만 완료 처리 가능
    if (matching.getMatchingStatus() != MatchingStatus.TBC) {
      throw new BusinessException(ErrorCode.MATCHING_002);
    }

    matching.complete(request.getBankTransactionId());
    matching.getContract().updateContractStatus(ContractStatus.PAID);

    return ManualMatchingResponse.builder()
        .matchingId(matching.getMatchingId())
        .matchingStatus(matching.getMatchingStatus())
        .matchedBy(matching.getMatchedBy())
        .matchedAt(matching.getMatchedAt())
        .build();
  }

  @Override
  public void processDeposit(
      Long userId, Long accountId, Long bankTransactionId, BigDecimal depositAmount) {

    // 입금계좌(INCOME)로 들어온 입금만 처리
    Optional<AccountMapping> incomeMapping =
        accountMappingRepository.findByUserIdAndMappingType(
            userId, AccountMapping.MappingType.INCOME);

    if (incomeMapping.isEmpty()) {
      log.debug("입금 처리 스킵 - INCOME 계좌 미연결: userId={}", userId);
      return;
    }

    Long incomeAccountId = incomeMapping.get().getLinkedFinancialAccount().getExternalAccountId();
    if (!incomeAccountId.equals(accountId)) {
      log.debug(
          "입금 처리 스킵 - 입금계좌 불일치: userId={}, accountId={}, incomeAccountId={}",
          userId,
          accountId,
          incomeAccountId);
      return;
    }

    List<PaymentMatching> tbcMatchings = paymentMatchingRepository.findTbcByUserId(userId);

    if (tbcMatchings.isEmpty()) {
      log.debug("입금 처리 스킵 - TBC 매칭 없음: userId={}", userId);
      return;
    }

    // 예상 수령액의 97% 이상이면 정상 입금으로 판단 (±3% 허용)
    for (PaymentMatching matching : tbcMatchings) {
      BigDecimal expectedIncome = matching.getContract().getSettlement().getActualIncome();
      BigDecimal lowerThreshold = expectedIncome.multiply(MATCH_THRESHOLD_RATE);
      BigDecimal upperThreshold = expectedIncome.multiply(new BigDecimal("1.03"));

      if (depositAmount.compareTo(lowerThreshold) >= 0 && depositAmount.compareTo(upperThreshold) <= 0) {
        matching.autoMatch(bankTransactionId, depositAmount);
        matching.getContract().updateContractStatus(ContractStatus.PAID);
        autoDistributionService.distribute(userId, matching.getMatchingId());
        log.info(
            "자동 매칭 완료: matchingId={}, depositAmount={}, expectedIncome={}",
            matching.getMatchingId(),
            depositAmount,
            expectedIncome);
        return;
      }
    }

    // 정상 금액 없음: 첫 번째 TBC 매칭에 입금 정보만 연결하고 TBC 유지
    PaymentMatching mismatchTarget = tbcMatchings.get(0);
    mismatchTarget.linkDeposit(bankTransactionId, depositAmount);
    log.info(
        "금액 불일치 - TBC 유지: matchingId={}, depositAmount={}, expectedIncome={}",
        mismatchTarget.getMatchingId(),
        depositAmount,
        mismatchTarget.getContract().getSettlement().getActualIncome());
  }

  @Override
  public void expireOverdueTbcMatchings() {
    LocalDate today = LocalDate.now();
    List<PaymentMatching> expired = paymentMatchingRepository.findExpiredTbc(today);

    for (PaymentMatching matching : expired) {
      matching.markFailed();
      matching.getContract().updateContractStatus(ContractStatus.DELAYED);
      log.info(
          "TBC 만료 → FAILED 처리: matchingId={}, contractId={}, expectedPaymentDate={}",
          matching.getMatchingId(),
          matching.getContract().getContractId(),
          matching.getContract().getExpectedPaymentDate());
    }
  }

  private PaymentMatchingResponse toResponse(PaymentMatching pm) {
    return PaymentMatchingResponse.builder()
        .matchingId(pm.getMatchingId())
        .contractId(pm.getContract().getContractId())
        .bankTransactionId(pm.getBankTransactionId())
        .transactionAmount(pm.getTransactionAmount())
        .matchingStatus(pm.getMatchingStatus())
        .matchedBy(pm.getMatchedBy())
        .matchedAt(pm.getMatchedAt())
        .build();
  }
}
