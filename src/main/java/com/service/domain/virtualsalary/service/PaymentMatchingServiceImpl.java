package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.dto.request.ManualMatchingRequest;
import com.service.domain.virtualsalary.dto.response.ManualMatchingResponse;
import com.service.domain.virtualsalary.dto.response.PaymentMatchingResponse;
import com.service.domain.virtualsalary.entity.Contract;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.repository.ContractRepository;
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentMatchingServiceImpl implements PaymentMatchingService {

  private static final BigDecimal MATCH_THRESHOLD_RATE = new BigDecimal("0.97");

  private final PaymentMatchingRepository paymentMatchingRepository;
  private final ContractRepository contractRepository;
  private final AutoDistributionService autoDistributionService;
  private final AccountMappingRepository accountMappingRepository;
  private final TransactionServerClient transactionServerClient;

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

    // 하한: actualIncome * 0.97, 상한: contractAmount * 1.03 (세전 전액 입금도 허용)
    for (PaymentMatching matching : tbcMatchings) {
      BigDecimal expectedIncome = matching.getContract().getSettlement().getActualIncome();
      BigDecimal contractAmount = matching.getContract().getContractAmount();
      BigDecimal lowerThreshold = expectedIncome.multiply(MATCH_THRESHOLD_RATE);
      BigDecimal upperThreshold = contractAmount.multiply(new BigDecimal("1.03"));

      if (depositAmount.compareTo(lowerThreshold) >= 0
          && depositAmount.compareTo(upperThreshold) <= 0) {
        matching.autoMatch(bankTransactionId, depositAmount);
        matching.getContract().updateContractStatus(ContractStatus.PAID);
        boolean distributed = false;
        try {
          distributed = autoDistributionService.distribute(userId, matching.getMatchingId());
        } catch (Exception e) {
          log.warn(
              "자동 분배 실패 (매칭은 저장됨): matchingId={}, error={}",
              matching.getMatchingId(),
              e.getMessage());
        }
        if (distributed) {
          matching.markDistributed();
        } else {
          log.warn(
              "자동 분배 스킵됨 (분배 없이 매칭만 저장): matchingId={}, userId={}",
              matching.getMatchingId(),
              userId);
        }
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

  @Override
  @Transactional(readOnly = true)
  public List<Long> findUsersWithTbcMatchings() {
    return paymentMatchingRepository.findDistinctUserIdsWithTbcMatchings();
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void pollAndMatchForUser(Long userId) {
    Optional<AccountMapping> incomeOpt =
        accountMappingRepository.findByUserIdAndMappingTypeFetch(
            userId, AccountMapping.MappingType.INCOME);
    if (incomeOpt.isEmpty()) {
      log.debug("입금 폴링 스킵 - INCOME 계좌 미연결: userId={}", userId);
      return;
    }

    Long accountId = incomeOpt.get().getLinkedFinancialAccount().getExternalAccountId();
    String fromDate = LocalDate.now().minusDays(30).toString();
    // toDate를 내일로 설정 — 오늘 입금 건이 서버에서 exclusive 처리될 경우에도 포함되도록 보장
    String toDate = LocalDate.now().plusDays(1).toString();

    log.info(
        "입금 폴링 - 조회 계좌: accountId={}, fromDate={}, toDate={}, userId={}",
        accountId,
        fromDate,
        toDate,
        userId);

    TransactionServerClient.TxPageData<TransactionServerClient.BankTransactionItem> result;
    try {
      result =
          transactionServerClient.getBankTransactions(accountId, null, fromDate, toDate, 0, 200);
    } catch (Exception e) {
      log.warn(
          "입금 폴링 실패 - 거래내역 조회 오류: userId={}, accountId={}, error={}",
          userId,
          accountId,
          e.getMessage());
      return;
    }

    if (result == null || result.getContent() == null || result.getContent().isEmpty()) {
      log.info("입금 폴링 - 조회된 거래 없음: userId={}, accountId={}", userId, accountId);
      return;
    }

    // 1. 성공 입금 건만 필터
    List<TransactionServerClient.BankTransactionItem> availableDeposits =
        result.getContent().stream()
            .filter(tx -> "SUCCESS".equals(tx.getTransactionStatus()))
            .filter(
                tx -> {
                  String t = tx.getTransactionType();
                  return "DEPOSIT".equals(t) || "TRANSFER_IN".equals(t);
                })
            .collect(Collectors.toList());

    log.info(
        "입금 폴링 - 전체 거래: {}, 성공 입금: {}: userId={}",
        result.getContent().size(),
        availableDeposits.size(),
        userId);

    if (availableDeposits.isEmpty()) return;

    // 2. 이미 MATCHED된 bankTransactionId 제외
    Set<Long> matchedTxIds = paymentMatchingRepository.findMatchedTransactionIdsByUserId(userId);
    availableDeposits.removeIf(tx -> matchedTxIds.contains(tx.getTransactionId()));

    log.info("입금 폴링 - MATCHED 제외 후 가용 입금: {}: userId={}", availableDeposits.size(), userId);

    if (availableDeposits.isEmpty()) return;

    // 3. TBC 매칭 목록 조회 (contract + settlement fetch join으로 lazy load 방지)
    List<PaymentMatching> tbcMatchings = paymentMatchingRepository.findTbcByUserIdFetch(userId);
    log.info("입금 폴링 - TBC 매칭 수: {}: userId={}", tbcMatchings.size(), userId);
    if (tbcMatchings.isEmpty()) return;

    // 4. ±3% 이내 후보 쌍 구성 (차액 기준 정렬)
    record Candidate(
        BigDecimal diff,
        PaymentMatching matching,
        TransactionServerClient.BankTransactionItem deposit) {}

    List<Candidate> candidates = new ArrayList<>();
    BigDecimal upperRate = new BigDecimal("1.03");

    for (PaymentMatching matching : tbcMatchings) {
      BigDecimal actualIncome = matching.getContract().getSettlement().getActualIncome();
      BigDecimal contractAmount = matching.getContract().getContractAmount();
      // 하한: actualIncome * 0.97 (세후 기준 허용 하한)
      // 상한: contractAmount * 1.03 (세전 전액 입금도 허용)
      BigDecimal lower = actualIncome.multiply(MATCH_THRESHOLD_RATE);
      BigDecimal upper = contractAmount.multiply(upperRate);

      for (TransactionServerClient.BankTransactionItem tx : availableDeposits) {
        BigDecimal amount = tx.getAmount();
        if (amount.compareTo(lower) >= 0 && amount.compareTo(upper) <= 0) {
          candidates.add(new Candidate(actualIncome.subtract(amount).abs(), matching, tx));
        } else {
          log.debug(
              "입금 폴링 - 범위 외 (matchingId={}, actualIncome={}, contractAmount={}, amount={}, range=[{}, {}])",
              matching.getMatchingId(),
              actualIncome,
              contractAmount,
              amount,
              lower,
              upper);
        }
      }
    }

    log.info("입금 폴링 - 후보 수: {}: userId={}", candidates.size(), userId);

    // 5. 차액 오름차순 그리디 배정
    candidates.sort(Comparator.comparing(Candidate::diff));
    Set<Long> usedMatchingIds = new HashSet<>();
    Set<Long> usedDepositIds = new HashSet<>();

    for (Candidate c : candidates) {
      Long mId = c.matching().getMatchingId();
      Long tId = c.deposit().getTransactionId();
      if (usedMatchingIds.contains(mId) || usedDepositIds.contains(tId)) continue;

      Contract contract = c.matching().getContract();
      c.matching().autoMatch(tId, c.deposit().getAmount());
      contract.updateContractStatus(ContractStatus.PAID);
      boolean distributed = false;
      try {
        distributed = autoDistributionService.distribute(userId, mId);
      } catch (Exception e) {
        log.warn("자동 분배 실패 (매칭은 저장됨): matchingId={}, error={}", mId, e.getMessage());
      }
      if (distributed) {
        c.matching().markDistributed();
      } else {
        log.warn("자동 분배 스킵됨 (분배 없이 매칭만 저장): matchingId={}, userId={}", mId, userId);
      }
      contractRepository.save(contract);
      paymentMatchingRepository.save(c.matching());
      usedMatchingIds.add(mId);
      usedDepositIds.add(tId);

      log.info(
          "자동 매칭 완료: matchingId={}, depositAmount={}, expectedIncome={}",
          mId,
          c.deposit().getAmount(),
          c.matching().getContract().getSettlement().getActualIncome());
    }

    // 6. 미매칭 TBC: 가장 가까운 입금 건 linkDeposit (불일치 표시용)
    for (PaymentMatching matching : tbcMatchings) {
      if (usedMatchingIds.contains(matching.getMatchingId())) continue;
      if (availableDeposits.isEmpty()) continue;

      BigDecimal expected = matching.getContract().getSettlement().getActualIncome();
      availableDeposits.stream()
          .filter(tx -> !usedDepositIds.contains(tx.getTransactionId()))
          .min(Comparator.comparing(tx -> tx.getAmount().subtract(expected).abs()))
          .ifPresent(
              closest -> {
                matching.linkDeposit(closest.getTransactionId(), closest.getAmount());
                paymentMatchingRepository.save(matching);
                log.info(
                    "금액 불일치 - TBC linkDeposit: matchingId={}, depositAmount={}, expectedIncome={}",
                    matching.getMatchingId(),
                    closest.getAmount(),
                    expected);
              });
    }
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void retryPendingDistributions() {
    List<PaymentMatching> pending = paymentMatchingRepository.findMatchedWithoutDistributionFetch();
    log.info("분배 재시도 시작: 미완료 건 수={}", pending.size());

    for (PaymentMatching matching : pending) {
      Long userId = matching.getContract().getUserId();
      Long matchingId = matching.getMatchingId();
      boolean distributed = false;
      try {
        distributed = autoDistributionService.distribute(userId, matchingId);
      } catch (Exception e) {
        log.warn(
            "분배 재시도 실패: matchingId={}, userId={}, error={}", matchingId, userId, e.getMessage());
      }
      if (distributed) {
        matching.markDistributed();
        paymentMatchingRepository.save(matching);
        log.info("분배 재시도 완료: matchingId={}, userId={}", matchingId, userId);
      }
    }

    log.info("분배 재시도 완료");
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void retryDistributionForUser(Long userId) {
    List<PaymentMatching> pending =
        paymentMatchingRepository.findMatchedWithoutDistributionByUserIdFetch(userId);
    log.info("분배 재시도(유저) 시작: userId={}, 미완료 건 수={}", userId, pending.size());

    for (PaymentMatching matching : pending) {
      Long matchingId = matching.getMatchingId();
      boolean distributed = false;
      try {
        distributed = autoDistributionService.distribute(userId, matchingId);
      } catch (Exception e) {
        log.warn(
            "분배 재시도 실패: matchingId={}, userId={}, error={}", matchingId, userId, e.getMessage());
      }
      if (distributed) {
        matching.markDistributed();
        paymentMatchingRepository.save(matching);
        log.info("분배 재시도 완료: matchingId={}, userId={}", matchingId, userId);
      }
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
