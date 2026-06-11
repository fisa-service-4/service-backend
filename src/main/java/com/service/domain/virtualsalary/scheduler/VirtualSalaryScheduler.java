package com.service.domain.virtualsalary.scheduler;

import com.service.domain.virtualsalary.service.PaymentMatchingService;
import com.service.domain.virtualsalary.service.VirtualSalaryPaymentService;
import java.time.LocalDate;
import java.util.List;
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

  // 매일 00:10(KST): 가상월급 지급일 처리
  @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
  public void runPaydayScheduler() {
    LocalDate today = LocalDate.now();
    int dayOfMonth = today.getDayOfMonth();
    boolean isLastDayOfMonth = dayOfMonth == today.lengthOfMonth();

    log.info("가상월급 스케줄러 실행: date={}, isLastDay={}", today, isLastDayOfMonth);
    virtualSalaryPaymentService.processPayday(dayOfMonth, isLastDayOfMonth);
  }

  // 매일 00:20(KST): 만료된 TBC 매칭을 FAILED로 전환 (bank 정합성 검증 00:00 완료 후)
  @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Seoul")
  public void expireOverdueTbcMatchings() {
    log.info("TBC 만료 체크 실행: date={}", LocalDate.now());
    paymentMatchingService.expireOverdueTbcMatchings();
  }

  // 12시간마다(00:30, 12:30 KST): TBC 보유 사용자의 입금 내역을 조회하여 자동 매칭
  @Scheduled(cron = "0 30 0,12 * * *", zone = "Asia/Seoul")
  public void pollDepositsAndMatch() {
    List<Long> userIds = paymentMatchingService.findUsersWithTbcMatchings();
    log.info("입금 매칭 폴링 시작: TBC 보유 사용자 수={}", userIds.size());

    for (Long userId : userIds) {
      try {
        paymentMatchingService.pollAndMatchForUser(userId);
      } catch (Exception e) {
        log.warn("입금 매칭 폴링 실패: userId={}, error={}", userId, e.getMessage());
      }
    }

    log.info("입금 매칭 폴링 완료");
  }

  // 매일 00:50(KST): 매칭 완료됐으나 분배 미완료 건 재시도
  @Scheduled(cron = "0 50 0 * * *", zone = "Asia/Seoul")
  public void retryPendingDistributions() {
    log.info("분배 재시도 스케줄러 실행: date={}", LocalDate.now());
    paymentMatchingService.retryPendingDistributions();
  }
}
