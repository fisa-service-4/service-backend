package com.service.domain.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.domain.analytics.entity.AnalysisRawTransaction;
import com.service.domain.analytics.repository.AnalysisRawTransactionRepository;
import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import com.service.domain.mydata.repository.IntegratedTransactionHistoryRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RawTransactionSyncService {

  private static final int BATCH_SIZE = 1000;

  private final IntegratedTransactionHistoryRepository integratedTxRepository;
  private final AnalysisRawTransactionRepository analysisRawTransactionRepository;
  private final ObjectMapper objectMapper;

  /**
   * 운영 DB의 INTEGRATED_TRANSACTION_HISTORY → 분석 DB의 ANALYSIS_RAW_TRANSACTION 동기화.
   *
   * <p>source_transaction_id(= integrated_transaction_id) 기준 워터마크 폴링 방식으로 중복 적재를 방지한다. 1회 최대
   * BATCH_SIZE건 처리하며, 다음 실행 주기에 이어서 동기화된다.
   */
  public void sync() {
    long lastSyncedId = analysisRawTransactionRepository.findMaxSourceTransactionId().orElse(0L);

    List<IntegratedTransactionHistory> newTxs =
        integratedTxRepository
            .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
                lastSyncedId, PageRequest.of(0, BATCH_SIZE));

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
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("integratedTransactionId", tx.getIntegratedTransactionId());
      payload.put("userId", tx.getUserId());
      payload.put("institutionType", tx.getInstitutionType());
      payload.put("transactionType", tx.getTransactionType());
      payload.put("transactionAmount", tx.getTransactionAmount());
      payload.put("balanceAfter", tx.getBalanceAfter());
      payload.put("transactionCategory", tx.getTransactionCategory());
      payload.put("merchantName", tx.getMerchantName());
      payload.put("originalTransactionId", tx.getOriginalTransactionId());
      payload.put(
          "transactionOccurredAt",
          tx.getTransactionOccurredAt() != null ? tx.getTransactionOccurredAt().toString() : null);
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      throw new IllegalStateException("JSON 직렬화 중 오류가 발생했습니다.", e);
    }
  }
}
