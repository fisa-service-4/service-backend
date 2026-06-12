package com.service.domain.analytics.scheduler;

import com.service.domain.analytics.service.AssetSnapshotService;
import com.service.domain.analytics.service.RawTransactionSyncService;
import com.service.global.client.AiServerClient;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsSyncScheduler {

  private final RawTransactionSyncService rawTransactionSyncService;
  private final AssetSnapshotService assetSnapshotService;
  private final AiServerClient aiServerClient;

  /** 5분마다 신규 거래내역을 분석 DB로 동기화 후 영향받은 사용자 AI 파이프라인 트리거 */
  @Scheduled(fixedDelay = 300000)
  public void syncRawTransactions() {
    log.info("거래내역 분석 DB 동기화 스케줄러 실행");
    try {
      Set<Long> affectedUserIds = rawTransactionSyncService.sync();
      affectedUserIds.forEach(aiServerClient::triggerPipeline);
    } catch (Exception e) {
      log.error("거래내역 분석 DB 동기화 실패: {}", e.getMessage(), e);
    }
  }

  /** 매일 새벽 1시 사용자별 자산 스냅샷 생성 */
  @Scheduled(cron = "0 0 16 * * *")
  public void createAssetSnapshots() {
    log.info("자산 스냅샷 생성 스케줄러 실행");
    try {
      assetSnapshotService.createSnapshots();
    } catch (Exception e) {
      log.error("자산 스냅샷 생성 실패: {}", e.getMessage(), e);
    }
  }
}
