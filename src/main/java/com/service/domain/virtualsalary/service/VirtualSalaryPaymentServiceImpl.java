package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.BankServerClient;
import com.service.global.client.TransactionServerClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualSalaryPaymentServiceImpl implements VirtualSalaryPaymentService {

  private final VirtualSalarySettingRepository virtualSalarySettingRepository;
  private final AccountMappingRepository accountMappingRepository;
  private final BankServerClient bankServerClient;
  private final TransactionServerClient transactionServerClient;

  @Override
  @Transactional(readOnly = true)
  public void processPayday(int today, boolean isLastDayOfMonth) {
    List<VirtualSalarySetting> targets = resolveTargets(today, isLastDayOfMonth);

    for (VirtualSalarySetting setting : targets) {
      processSingleUser(setting);
    }
  }

  private List<VirtualSalarySetting> resolveTargets(int today, boolean isLastDayOfMonth) {
    List<VirtualSalarySetting> settings =
        new ArrayList<>(virtualSalarySettingRepository.findAllByPayday(today));

    if (isLastDayOfMonth) {
      // 이번 달 말일이 payday보다 빠른 경우(예: 2월 28일, payday=29/30/31) 말일에 함께 처리
      settings.addAll(virtualSalarySettingRepository.findAllByPaydayGreaterThan(today));
    }

    return settings;
  }

  private void processSingleUser(VirtualSalarySetting setting) {
    Long userId = setting.getUserId();

    Optional<AccountMapping> incomeMapping =
        accountMappingRepository.findByUserIdAndMappingType(
            userId, AccountMapping.MappingType.INCOME);
    Optional<AccountMapping> salaryMapping =
        accountMappingRepository.findByUserIdAndMappingType(
            userId, AccountMapping.MappingType.SALARY);

    if (incomeMapping.isEmpty() || salaryMapping.isEmpty()) {
      log.warn("가상월급 지급 스킵 - 계좌 미연결: userId={}", userId);
      return;
    }

    Long incomeAccountId = incomeMapping.get().getLinkedFinancialAccount().getExternalAccountId();
    Long salaryAccountId = salaryMapping.get().getLinkedFinancialAccount().getExternalAccountId();

    BigDecimal incomeBalance;
    try {
      incomeBalance = bankServerClient.getAccountBalance(incomeAccountId);
    } catch (Exception e) {
      log.warn("가상월급 지급 스킵 - INCOME 잔액 조회 실패: userId={}", userId);
      return;
    }

    BigDecimal targetSalary = setting.getTargetSalary();
    BigDecimal paidAmount = targetSalary.min(incomeBalance);

    if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
      log.warn("가상월급 지급 스킵 - INCOME 잔액 없음: userId={}", userId);
      return;
    }

    try {
      BankServerClient.BankAccountDetailData salaryDetail =
          bankServerClient.getBankAccountDetail(salaryAccountId);

      // Issue 5 수정: 결정론적 키 → 같은 날 스케줄러 재실행 시 중복 이체 방지
      String idempotencyKey = "payday-" + userId + "-" + LocalDate.now();
      transactionServerClient.bankTransfer(
          idempotencyKey,
          incomeAccountId,
          salaryDetail.getBankCode(),
          salaryDetail.getAccountNumber(),
          paidAmount,
          "AI");

      log.info(
          "가상월급 지급 완료: userId={}, targetSalary={}, paidAmount={}",
          userId,
          targetSalary,
          paidAmount);
    } catch (Exception e) {
      log.warn("가상월급 지급 실패: userId={}, error={}", userId, e.getMessage());
    }
  }
}
