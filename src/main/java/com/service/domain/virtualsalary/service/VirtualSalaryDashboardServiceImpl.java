package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.dto.response.VirtualSalaryDashboardResponse;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.BankServerClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
    Optional<VirtualSalarySetting> settingOpt = virtualSalarySettingRepository.findById(userId);

    if (settingOpt.isEmpty()) {
      return VirtualSalaryDashboardResponse.builder()
          .targetSalary(BigDecimal.ZERO)
          .currentBalance(BigDecimal.ZERO)
          .remainAmount(BigDecimal.ZERO)
          .usedAmount(BigDecimal.ZERO)
          .progressRate(BigDecimal.ZERO)
          .payday(0)
          .dday(0L)
          .build();
    }

    VirtualSalarySetting setting = settingOpt.get();

    Optional<AccountMapping> salaryMappingOpt =
        accountMappingRepository.findByUserIdAndMappingType(
            userId, AccountMapping.MappingType.SALARY);

    if (salaryMappingOpt.isEmpty()) {
      return VirtualSalaryDashboardResponse.builder()
          .targetSalary(setting.getTargetSalary())
          .currentBalance(BigDecimal.ZERO)
          .remainAmount(setting.getTargetSalary())
          .usedAmount(BigDecimal.ZERO)
          .progressRate(BigDecimal.ZERO)
          .payday(setting.getPayday())
          .dday(calculateDday(setting.getPayday()))
          .build();
    }

    Long externalAccountId =
        salaryMappingOpt.get().getLinkedFinancialAccount().getExternalAccountId();
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
