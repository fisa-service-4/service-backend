package com.service.domain.mydata.scheduler;

import com.service.domain.mydata.service.TransactionSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionSyncScheduler {

  private final TransactionSyncService transactionSyncService;

  @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
  @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
  public void sync() {
    transactionSyncService.syncAll();
  }
}
