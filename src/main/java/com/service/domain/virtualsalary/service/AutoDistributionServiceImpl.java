package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.enumtype.VirtualSalaryCategory;
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.BankServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AutoDistributionServiceImpl implements AutoDistributionService {

  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final PaymentMatchingRepository paymentMatchingRepository;
  private final VirtualSalarySettingRepository virtualSalarySettingRepository;
  private final AccountMappingRepository accountMappingRepository;
  private final BankServerClient bankServerClient;

  @Override
  public void distribute(Long userId, Long matchingId) {
    VirtualSalarySetting setting = virtualSalarySettingRepository.findById(userId).orElse(null);
    if (setting == null) return;

    PaymentMatching matching =
        paymentMatchingRepository
            .findById(matchingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MATCHING_001));

    BigDecimal actualIncome = matching.getContract().getSettlement().getActualIncome();
    BigDecimal incomeBalance = resolveIncomeBalance(userId);

    calculate(actualIncome, incomeBalance, setting);
  }

  private DistributionResult calculate(
      BigDecimal actualIncome, BigDecimal incomeBalance, VirtualSalarySetting setting) {

    List<VirtualSalaryCategory> order = setting.getPriorityOrder();
    BigDecimal distributable = actualIncome;
    BigDecimal salaryReserved = ZERO;
    BigDecimal emergencyAmount = ZERO;
    BigDecimal investmentAmount = ZERO;

    BigDecimal emergencyCapRemaining = resolveEmergencyCapRemaining(setting);

    if (order != null && !order.isEmpty()) {
      for (VirtualSalaryCategory cat : order) {
        switch (cat) {
          case SALARY -> {
            salaryReserved = setting.getTargetSalary().min(distributable);
            distributable = distributable.subtract(salaryReserved);
          }
          case EMERGENCY -> {
            if (setting.getEmergencyRatio() != null && emergencyCapRemaining.compareTo(ZERO) > 0) {
              BigDecimal calc =
                  actualIncome
                      .multiply(setting.getEmergencyRatio())
                      .divide(HUNDRED, 2, RoundingMode.DOWN);
              emergencyAmount = calc.min(distributable).min(emergencyCapRemaining);
              distributable = distributable.subtract(emergencyAmount);
            }
          }
          case INVESTMENT -> {
            if (setting.getInvestmentRatio() != null) {
              BigDecimal calc =
                  actualIncome
                      .multiply(setting.getInvestmentRatio())
                      .divide(HUNDRED, 2, RoundingMode.DOWN);
              investmentAmount = calc.min(distributable);
              distributable = distributable.subtract(investmentAmount);
            }
          }
        }
      }
    } else {
      emergencyAmount = calcEmergencyRaw(actualIncome, setting, emergencyCapRemaining);
      investmentAmount =
          calcInvestmentRaw(actualIncome, setting, actualIncome.subtract(emergencyAmount));
      distributable = actualIncome.subtract(emergencyAmount).subtract(investmentAmount);
    }

    BigDecimal livingAmount = distributable;

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

  private BigDecimal resolveEmergencyCapRemaining(VirtualSalarySetting setting) {
    if (setting.getEmergencyTargetAmount() == null) return BigDecimal.valueOf(Long.MAX_VALUE);
    return setting.getEmergencyTargetAmount().max(ZERO);
  }

  private BigDecimal resolveIncomeBalance(Long userId) {
    try {
      return accountMappingRepository
          .findByUserIdAndMappingType(userId, AccountMapping.MappingType.INCOME)
          .map(
              m ->
                  bankServerClient.getAccountBalance(
                      m.getLinkedFinancialAccount().getExternalAccountId()))
          .orElse(BigDecimal.valueOf(Long.MAX_VALUE));
    } catch (Exception e) {
      return BigDecimal.valueOf(Long.MAX_VALUE);
    }
  }

  private BigDecimal calcEmergencyRaw(
      BigDecimal base, VirtualSalarySetting setting, BigDecimal cap) {
    if (setting.getEmergencyRatio() == null || cap.compareTo(ZERO) <= 0) return ZERO;
    BigDecimal calc =
        base.multiply(setting.getEmergencyRatio()).divide(HUNDRED, 2, RoundingMode.DOWN);
    return calc.min(cap);
  }

  private BigDecimal calcInvestmentRaw(
      BigDecimal base, VirtualSalarySetting setting, BigDecimal distributable) {
    if (setting.getInvestmentRatio() == null) return ZERO;
    BigDecimal calc =
        base.multiply(setting.getInvestmentRatio()).divide(HUNDRED, 2, RoundingMode.DOWN);
    return calc.min(distributable);
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
