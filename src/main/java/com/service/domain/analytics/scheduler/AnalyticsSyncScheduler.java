package com.service.domain.analytics.scheduler;

import com.service.domain.analytics.service.RawTransactionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsSyncScheduler {

  private final RawTransactionSyncService rawTransactionSyncService;

  /** 1분마다 신규 거래내역을 분석 DB로 동기화 */
  @Scheduled(fixedDelay = 60_000)
  public void syncRawTransactions() {
    log.info("거래내역 분석 DB 동기화 스케줄러 실행");
    try {
      rawTransactionSyncService.sync();
    } catch (Exception e) {
      log.error("거래내역 분석 DB 동기화 실패: {}", e.getMessage(), e);
    }
  }
}
