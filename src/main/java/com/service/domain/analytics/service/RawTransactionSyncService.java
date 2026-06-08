package com.service.domain.analytics.service;

import com.service.domain.analytics.entity.AnalysisRawTransaction;
import com.service.domain.analytics.repository.AnalysisRawTransactionRepository;
import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import com.service.domain.mydata.repository.IntegratedTransactionHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RawTransactionSyncService {

  private final IntegratedTransactionHistoryRepository integratedTxRepository;
  private final AnalysisRawTransactionRepository analysisRawTransactionRepository;

  /**
   * 운영 DB의 INTEGRATED_TRANSACTION_HISTORY → 분석 DB의 ANALYSIS_RAW_TRANSACTION 동기화.
   *
   * <p>source_transaction_id(= integrated_transaction_id) 기준 워터마크 폴링 방식으로 중복 적재를 방지한다.
   */
  public void sync() {
    long lastSyncedId = analysisRawTransactionRepository.findMaxSourceTransactionId().orElse(0L);

    List<IntegratedTransactionHistory> newTxs =
        integratedTxRepository
            .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
                lastSyncedId);

    if (newTxs.isEmpty()) {
      log.debug("동기화할 신규 거래내역 없음 (lastSyncedId={})", lastSyncedId);
      return;
    }

    log.info("거래내역 분석 DB 동기화 시작: {}건 (lastSyncedId={})", newTxs.size(), lastSyncedId);
    List<AnalysisRawTransaction> records =
        newTxs.stream().map(this::toAnalysisRawTransaction).toList();
    analysisRawTransactionRepository.saveAll(records);
    log.info("거래내역 분석 DB 동기화 완료: {}건", records.size());
  }

  private AnalysisRawTransaction toAnalysisRawTransaction(IntegratedTransactionHistory tx) {
    return AnalysisRawTransaction.builder()
        .userId(tx.getUserId())
        .sourceType(tx.getInstitutionType())
        .sourceTransactionId(tx.getIntegratedTransactionId())
        .accountId(tx.getLinkedAccountId())
        .transactionType(tx.getTransactionType())
        .category(tx.getTransactionCategory())
        .amount(tx.getTransactionAmount())
        .balanceAfter(tx.getBalanceAfter())
        .transactionAt(tx.getTransactionOccurredAt())
        .rawPayload(buildRawPayload(tx))
        .syncedAt(LocalDateTime.now())
        .build();
  }

  private String buildRawPayload(IntegratedTransactionHistory tx) {
    String category =
        tx.getTransactionCategory() != null ? "\"" + tx.getTransactionCategory() + "\"" : "null";
    String merchantName =
        tx.getMerchantName() != null ? "\"" + tx.getMerchantName() + "\"" : "null";
    return String.format(
        "{\"integratedTransactionId\":%d,\"userId\":%d,\"institutionType\":\"%s\","
            + "\"transactionType\":\"%s\",\"transactionAmount\":%s,"
            + "\"balanceAfter\":%s,\"transactionCategory\":%s,"
            + "\"merchantName\":%s,\"originalTransactionId\":%d,"
            + "\"transactionOccurredAt\":\"%s\"}",
        tx.getIntegratedTransactionId(),
        tx.getUserId(),
        tx.getInstitutionType(),
        tx.getTransactionType(),
        tx.getTransactionAmount().toPlainString(),
        tx.getBalanceAfter() != null ? tx.getBalanceAfter().toPlainString() : "null",
        category,
        merchantName,
        tx.getOriginalTransactionId(),
        tx.getTransactionOccurredAt());
  }
}
