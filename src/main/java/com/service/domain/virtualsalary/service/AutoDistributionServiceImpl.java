package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.enumtype.VirtualSalaryCategory;
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.BankServerClient;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AutoDistributionServiceImpl implements AutoDistributionService {

  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final PaymentMatchingRepository paymentMatchingRepository;
  private final VirtualSalarySettingRepository virtualSalarySettingRepository;
  private final AccountMappingRepository accountMappingRepository;
  private final UserRepository userRepository;
  private final BankServerClient bankServerClient;
  private final TransactionServerClient transactionServerClient;

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public boolean distribute(Long userId, Long matchingId) {
    VirtualSalarySetting setting = virtualSalarySettingRepository.findById(userId).orElse(null);
    if (setting == null) {
      log.warn("자동 분배 스킵 - 가상월급 설정 없음 (POST /virtual-salary 필요): userId={}", userId);
      return false;
    }

    PaymentMatching matching =
        paymentMatchingRepository
            .findByIdWithContractAndSettlement(matchingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MATCHING_001));

    Long incomeAccountId = resolveAccountId(userId, AccountMapping.MappingType.INCOME);
    if (incomeAccountId == null) {
      log.warn("자동 분배 스킵 - INCOME 계좌 미연결: userId={}", userId);
      return false;
    }

    BigDecimal incomeBalance;
    try {
      incomeBalance = bankServerClient.getAccountBalance(incomeAccountId);
    } catch (Exception e) {
      log.warn("자동 분배 스킵 - INCOME 잔액 조회 실패: userId={}", userId);
      return false;
    }

    BigDecimal actualIncome = matching.getContract().getSettlement().getActualIncome();
    DistributionResult result = calculate(actualIncome, incomeBalance, setting);

    if (result.emergencyAmount().compareTo(ZERO) == 0
        && result.investmentAmount().compareTo(ZERO) == 0) {
      log.warn(
          "자동 분배 - 이체 금액 0원: 비상금/투자 이체 없음 (설정 미입력 또는 잔액 부족). userId={}, matchingId={}"
              + " | VirtualSalarySetting: emergencyAmount={}, investmentAmount={}",
          userId,
          matchingId,
          setting.getEmergencyAmount(),
          setting.getInvestmentAmount());
    }

    executeTransfers(userId, matchingId, incomeAccountId, result, setting);

    log.info(
        "자동 분배 완료: userId={}, matchingId={}, emergency={}, investment={}, living={}",
        userId,
        matchingId,
        result.emergencyAmount(),
        result.investmentAmount(),
        result.livingAmount());
    return true;
  }

  private void executeTransfers(
      Long userId,
      Long matchingId,
      Long incomeAccountId,
      DistributionResult result,
      VirtualSalarySetting setting) {

    if (result.emergencyAmount().compareTo(ZERO) > 0) {
      Long emergencyAccountId = resolveAccountId(userId, AccountMapping.MappingType.EMERGENCY);
      if (emergencyAccountId != null) {
        try {
          BigDecimal actualEmergencyAmount = result.emergencyAmount();

          if (setting.getEmergencyTargetAmount() != null) {
            BigDecimal currentEmergencyBalance =
                bankServerClient.getAccountBalance(emergencyAccountId);
            BigDecimal remaining =
                setting.getEmergencyTargetAmount().subtract(currentEmergencyBalance).max(ZERO);
            actualEmergencyAmount = actualEmergencyAmount.min(remaining);
          }

          if (actualEmergencyAmount.compareTo(ZERO) > 0) {
            BankServerClient.BankAccountDetailData emergencyDetail =
                bankServerClient.getBankAccountDetail(emergencyAccountId);
            TransactionServerClient.BankTransferResult emergencyTransfer =
                transactionServerClient.bankTransfer(
                    matchingId + "_EMERGENCY",
                    incomeAccountId,
                    emergencyDetail.getBankCode(),
                    emergencyDetail.getAccountNumber(),
                    actualEmergencyAmount,
                    "AI");
            transactionServerClient.approveTransfer(emergencyTransfer.getTransferId());
            log.info("비상금 이체 완료: userId={}, amount={}", userId, actualEmergencyAmount);
          } else {
            log.info("비상금 이체 스킵 - 목표 금액 달성: userId={}", userId);
          }
        } catch (Exception e) {
          log.warn("비상금 이체 실패: userId={}, error={}", userId, e.getMessage());
        }
      }
    }

    if (result.investmentAmount().compareTo(ZERO) > 0) {
      Long investmentAccountId = resolveAccountId(userId, AccountMapping.MappingType.STOCK);
      if (investmentAccountId != null) {
        try {
          String firebaseUid =
              userRepository
                  .findById(userId)
                  .map(u -> u.getFirebaseUid())
                  .orElseThrow(() -> new RuntimeException("사용자 조회 실패: userId=" + userId));
          BankServerClient.StockAccountItem investmentDetail =
              bankServerClient.getStockAccountDetail(firebaseUid, investmentAccountId);
          TransactionServerClient.BankTransferResult investmentTransfer =
              transactionServerClient.bankTransfer(
                  matchingId + "_INVESTMENT",
                  incomeAccountId,
                  investmentDetail.getBankCode(),
                  investmentDetail.getAccountNumber(),
                  result.investmentAmount(),
                  "AI");
          transactionServerClient.approveTransfer(investmentTransfer.getTransferId());
          log.info("투자 이체 완료: userId={}, amount={}", userId, result.investmentAmount());
        } catch (Exception e) {
          log.warn("투자 이체 실패: userId={}, error={}", userId, e.getMessage());
        }
      }
    }
  }

  private DistributionResult calculate(
      BigDecimal actualIncome, BigDecimal incomeBalance, VirtualSalarySetting setting) {

    List<VirtualSalaryCategory> order = setting.getPriorityOrder();
    BigDecimal distributable = actualIncome;
    BigDecimal salaryReserved = ZERO;
    BigDecimal emergencyAmount = ZERO;
    BigDecimal investmentAmount = ZERO;

    // Issue 2 수정: calculate()에서 emergencyCapRemaining 제거 — 실제 cap은 executeTransfers()에서 처리
    if (order != null && !order.isEmpty()) {
      for (VirtualSalaryCategory cat : order) {
        switch (cat) {
          case SALARY -> {
            salaryReserved = setting.getTargetSalary().min(distributable);
            distributable = distributable.subtract(salaryReserved);
          }
          case EMERGENCY -> {
            if (setting.getEmergencyAmount() != null) {
              emergencyAmount = setting.getEmergencyAmount().min(distributable);
              distributable = distributable.subtract(emergencyAmount);
            }
          }
          case INVESTMENT -> {
            if (setting.getInvestmentAmount() != null) {
              investmentAmount = setting.getInvestmentAmount().min(distributable);
              distributable = distributable.subtract(investmentAmount);
            }
          }
        }
      }
    } else {
      if (setting.getEmergencyAmount() != null) {
        emergencyAmount = setting.getEmergencyAmount().min(distributable);
        distributable = distributable.subtract(emergencyAmount);
      }
      if (setting.getInvestmentAmount() != null) {
        investmentAmount = setting.getInvestmentAmount().min(distributable);
        distributable = distributable.subtract(investmentAmount);
      }
    }

    BigDecimal livingAmount = distributable;

    // SALARY 우선순위 → 입금통장에서 salaryReserved를 제외한 잔액 기준으로 이체 가능 금액 cap
    BigDecimal effectiveBalance = incomeBalance.subtract(salaryReserved).max(ZERO);
    BigDecimal totalToTransfer = emergencyAmount.add(investmentAmount);

    if (effectiveBalance.compareTo(totalToTransfer) < 0) {
      boolean emergencyFirst = emergencyBeforeInvestment(order);
      BigDecimal[] capped =
          capByBalance(emergencyAmount, investmentAmount, effectiveBalance, emergencyFirst);
      livingAmount =
          livingAmount
              .add(emergencyAmount.subtract(capped[0]))
              .add(investmentAmount.subtract(capped[1]));
      emergencyAmount = capped[0];
      investmentAmount = capped[1];
    }

    return new DistributionResult(salaryReserved, emergencyAmount, investmentAmount, livingAmount);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private Long resolveAccountId(Long userId, AccountMapping.MappingType type) {
    return accountMappingRepository
        .findByUserIdAndMappingTypeFetch(userId, type)
        .map(m -> m.getLinkedFinancialAccount().getExternalAccountId())
        .orElse(null);
  }

  private BigDecimal[] capByBalance(
      BigDecimal emergency, BigDecimal investment, BigDecimal available, boolean emergencyFirst) {
    if (emergencyFirst) {
      BigDecimal e = emergency.min(available);
      BigDecimal i = investment.min(available.subtract(e).max(ZERO));
      return new BigDecimal[] {e, i};
    } else {
      BigDecimal i = investment.min(available);
      BigDecimal e = emergency.min(available.subtract(i).max(ZERO));
      return new BigDecimal[] {e, i};
    }
  }

  private boolean emergencyBeforeInvestment(List<VirtualSalaryCategory> order) {
    if (order == null) return true;
    int eIdx = order.indexOf(VirtualSalaryCategory.EMERGENCY);
    int iIdx = order.indexOf(VirtualSalaryCategory.INVESTMENT);
    if (eIdx == -1) return false;
    if (iIdx == -1) return true;
    return eIdx < iIdx;
  }

  private record DistributionResult(
      BigDecimal salaryReserved,
      BigDecimal emergencyAmount,
      BigDecimal investmentAmount,
      BigDecimal livingAmount) {}
}
