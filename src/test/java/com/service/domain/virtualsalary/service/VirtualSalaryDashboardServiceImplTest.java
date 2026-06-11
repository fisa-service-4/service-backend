package com.service.domain.virtualsalary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.entity.LinkedFinancialAccount;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.dto.response.VirtualSalaryDashboardResponse;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.BankServerClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VirtualSalaryDashboardServiceImplTest {

  @InjectMocks private VirtualSalaryDashboardServiceImpl dashboardService;

  @Mock private VirtualSalarySettingRepository settingRepository;

  @Mock private AccountMappingRepository accountMappingRepository;

  @Mock private BankServerClient bankServerClient;

  @Test
  @DisplayName("가상월급 설정이 없으면 모든 금액이 0인 기본 응답을 반환한다")
  void getDashboard_returnsZeroValuesWhenNoSetting() {
    given(settingRepository.findById(1L)).willReturn(Optional.empty());

    VirtualSalaryDashboardResponse response = dashboardService.getDashboard(1L);

    assertThat(response.getTargetSalary()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getRemainAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getUsedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getProgressRate()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getPayday()).isEqualTo(0);
    assertThat(response.getDday()).isEqualTo(0L);
  }

  @Test
  @DisplayName("SALARY 계좌가 미연결이면 currentBalance=0, remainAmount=targetSalary를 반환한다")
  void getDashboard_returnsTargetSalaryAsRemainWhenNoSalaryAccount() {
    VirtualSalarySetting setting =
        VirtualSalarySetting.builder()
            .userId(1L)
            .targetSalary(new BigDecimal("3000000"))
            .payday(25)
            .build();
    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.SALARY))
        .willReturn(Optional.empty());

    VirtualSalaryDashboardResponse response = dashboardService.getDashboard(1L);

    assertThat(response.getTargetSalary()).isEqualByComparingTo(new BigDecimal("3000000"));
    assertThat(response.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getRemainAmount()).isEqualByComparingTo(new BigDecimal("3000000"));
    assertThat(response.getProgressRate()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("목표월급 대비 사용 금액과 진행률이 올바르게 계산된다")
  void getDashboard_calculatesProgressRateCorrectly() {
    VirtualSalarySetting setting =
        VirtualSalarySetting.builder()
            .userId(1L)
            .targetSalary(new BigDecimal("3000000"))
            .payday(25)
            .build();
    AccountMapping salaryMapping = buildAccountMapping(1001L);

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.SALARY))
        .willReturn(Optional.of(salaryMapping));
    given(bankServerClient.getAccountBalance(1001L)).willReturn(new BigDecimal("1200000"));

    VirtualSalaryDashboardResponse response = dashboardService.getDashboard(1L);

    // usedAmount = 3,000,000 - 1,200,000 = 1,800,000
    // progressRate = 1,800,000 / 3,000,000 * 100 = 60.00
    assertThat(response.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("1200000"));
    assertThat(response.getUsedAmount()).isEqualByComparingTo(new BigDecimal("1800000"));
    assertThat(response.getProgressRate()).isEqualByComparingTo(new BigDecimal("60.00"));
  }

  @Test
  @DisplayName("현재 잔액이 목표월급보다 많으면 usedAmount는 0을 반환한다")
  void getDashboard_returnsZeroUsedAmountWhenBalanceExceedsTarget() {
    VirtualSalarySetting setting =
        VirtualSalarySetting.builder()
            .userId(1L)
            .targetSalary(new BigDecimal("3000000"))
            .payday(25)
            .build();
    AccountMapping salaryMapping = buildAccountMapping(1001L);

    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.SALARY))
        .willReturn(Optional.of(salaryMapping));
    given(bankServerClient.getAccountBalance(1001L)).willReturn(new BigDecimal("4000000"));

    VirtualSalaryDashboardResponse response = dashboardService.getDashboard(1L);

    assertThat(response.getUsedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("payday가 오늘 이후이면 dday는 양수이다")
  void getDashboard_returnPositiveDdayWhenPaydayIsInFuture() {
    int futurePayday = LocalDate.now().plusDays(5).getDayOfMonth();
    VirtualSalarySetting setting =
        VirtualSalarySetting.builder()
            .userId(1L)
            .targetSalary(new BigDecimal("3000000"))
            .payday(futurePayday)
            .build();
    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));
    given(
            accountMappingRepository.findByUserIdAndMappingType(
                1L, AccountMapping.MappingType.SALARY))
        .willReturn(Optional.empty());

    VirtualSalaryDashboardResponse response = dashboardService.getDashboard(1L);

    assertThat(response.getDday()).isPositive();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

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
        .mappingType(AccountMapping.MappingType.SALARY)
        .build();
  }
}
