package com.service.domain.virtualsalary.scheduler;

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

  @Scheduled(cron = "0 0 9 * * *")
  public void runPaydayScheduler() {
    LocalDate today = LocalDate.now();
    int dayOfMonth = today.getDayOfMonth();
    boolean isLastDayOfMonth = dayOfMonth == today.lengthOfMonth();

    log.info("가상월급 스케줄러 실행: date={}, isLastDay={}", today, isLastDayOfMonth);
    virtualSalaryPaymentService.processPayday(dayOfMonth, isLastDayOfMonth);
  }
}
