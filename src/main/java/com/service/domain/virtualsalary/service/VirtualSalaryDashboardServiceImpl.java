package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.dto.response.VirtualSalaryDashboardResponse;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.BankServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VirtualSalaryDashboardServiceImpl implements VirtualSalaryDashboardService {

  private final VirtualSalarySettingRepository virtualSalarySettingRepository;
  private final AccountMappingRepository accountMappingRepository;
  private final BankServerClient bankServerClient;

  @Override
  public VirtualSalaryDashboardResponse getDashboard(Long userId) {
    VirtualSalarySetting setting =
        virtualSalarySettingRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VIRTUAL_SALARY_001));

    AccountMapping salaryMapping =
        accountMappingRepository
            .findByUserIdAndMappingType(userId, AccountMapping.MappingType.SALARY)
            .orElseThrow(() -> new BusinessException(ErrorCode.VIRTUAL_SALARY_003));

    Long externalAccountId = salaryMapping.getLinkedFinancialAccount().getExternalAccountId();
    BigDecimal currentBalance = bankServerClient.getAccountBalance(externalAccountId);

    BigDecimal targetSalary = setting.getTargetSalary();
    BigDecimal usedAmount = targetSalary.subtract(currentBalance).max(BigDecimal.ZERO);
    BigDecimal progressRate =
        targetSalary.compareTo(BigDecimal.ZERO) > 0
            ? usedAmount
                .divide(targetSalary, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    return VirtualSalaryDashboardResponse.builder()
        .targetSalary(targetSalary)
        .currentBalance(currentBalance)
        .remainAmount(currentBalance)
        .usedAmount(usedAmount)
        .progressRate(progressRate)
        .payday(setting.getPayday())
        .dday(calculateDday(setting.getPayday()))
        .build();
  }

  private long calculateDday(int payday) {
    LocalDate today = LocalDate.now();
    LocalDate targetPayday = safeWithDayOfMonth(today, payday);
    if (!targetPayday.isAfter(today)) {
      targetPayday = safeWithDayOfMonth(today.plusMonths(1), payday);
    }
    return ChronoUnit.DAYS.between(today, targetPayday);
  }

  private LocalDate safeWithDayOfMonth(LocalDate base, int day) {
    try {
      return base.withDayOfMonth(day);
    } catch (DateTimeException e) {
      return base.withDayOfMonth(base.lengthOfMonth());
    }
  }
}
