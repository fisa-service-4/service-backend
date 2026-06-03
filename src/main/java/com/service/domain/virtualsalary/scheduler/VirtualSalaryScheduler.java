package com.service.domain.virtualsalary.scheduler;

import com.service.domain.virtualsalary.service.PaymentMatchingService;
import com.service.domain.virtualsalary.service.VirtualSalaryPaymentService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VirtualSalaryScheduler {

  private final VirtualSalaryPaymentService virtualSalaryPaymentService;
  private final PaymentMatchingService paymentMatchingService;

  @Scheduled(cron = "0 0 9 * * *")
  public void runPaydayScheduler() {
    LocalDate today = LocalDate.now();
    int dayOfMonth = today.getDayOfMonth();
    boolean isLastDayOfMonth = dayOfMonth == today.lengthOfMonth();

    log.info("가상월급 스케줄러 실행: date={}, isLastDay={}", today, isLastDayOfMonth);
    virtualSalaryPaymentService.processPayday(dayOfMonth, isLastDayOfMonth);
  }

  // 매일 자정에 만료된 TBC 매칭을 FAILED로 전환
  @Scheduled(cron = "0 0 0 * * *")
  public void expireOverdueTbcMatchings() {
    log.info("TBC 만료 체크 실행: date={}", LocalDate.now());
    paymentMatchingService.expireOverdueTbcMatchings();
  }
}
