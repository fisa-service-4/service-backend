package com.service.domain.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.domain.analytics.entity.AnalysisRawTransaction;
import com.service.domain.analytics.repository.AnalysisRawTransactionRepository;
import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import com.service.domain.mydata.repository.IntegratedTransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RawTransactionSyncServiceTest {

  @InjectMocks private RawTransactionSyncService syncService;

  @Mock private IntegratedTransactionHistoryRepository integratedTxRepository;

  @Mock private AnalysisRawTransactionRepository analysisRawTransactionRepository;

  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("동기화할 신규 거래내역이 없으면 saveAll을 호출하지 않는다")
  void sync_skipsWhenNoNewTransactions() {
    given(analysisRawTransactionRepository.findMaxSourceTransactionId())
        .willReturn(Optional.of(100L));
    given(
            integratedTxRepository
                .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
                    eq(100L), any(Pageable.class)))
        .willReturn(Collections.emptyList());

    syncService.sync();

    then(analysisRawTransactionRepository).should(never()).saveAll(anyList());
  }

  @Test
  @DisplayName("최초 실행 시 워터마크가 없으면 ID > 0 기준으로 조회한다")
  void sync_usesZeroAsWatermarkOnFirstRun() {
    given(analysisRawTransactionRepository.findMaxSourceTransactionId())
        .willReturn(Optional.empty());
    given(
            integratedTxRepository
                .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
                    eq(0L), any(Pageable.class)))
        .willReturn(Collections.emptyList());

    syncService.sync();

    then(integratedTxRepository)
        .should()
        .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
            eq(0L), any(Pageable.class));
  }

  @Test
  @DisplayName("마지막 동기화 ID 이후의 거래내역만 조회한다")
  void sync_fetchesTransactionsAfterLastSyncedId() {
    given(analysisRawTransactionRepository.findMaxSourceTransactionId())
        .willReturn(Optional.of(500L));
    given(
            integratedTxRepository
                .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
                    eq(500L), any(Pageable.class)))
        .willReturn(Collections.emptyList());

    syncService.sync();

    then(integratedTxRepository)
        .should()
        .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
            eq(500L), any(Pageable.class));
  }

  @Test
  @DisplayName("신규 거래내역을 AnalysisRawTransaction으로 변환하여 saveAll을 호출한다")
  void sync_savesAllNewTransactionsAsMappedRecords() {
    given(analysisRawTransactionRepository.findMaxSourceTransactionId())
        .willReturn(Optional.of(0L));
    List<IntegratedTransactionHistory> txs =
        List.of(buildTx(1L, 101L), buildTx(2L, 102L), buildTx(3L, 103L));
    given(
            integratedTxRepository
                .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
                    eq(0L), any(Pageable.class)))
        .willReturn(txs);
    given(analysisRawTransactionRepository.saveAll(anyList()))
        .willAnswer(inv -> inv.getArgument(0));

    syncService.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AnalysisRawTransaction>> captor = ArgumentCaptor.forClass(List.class);
    then(analysisRawTransactionRepository).should().saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(3);
  }

  @Test
  @DisplayName("거래내역이 AnalysisRawTransaction의 필드에 정확히 매핑된다")
  void sync_mapsTransactionFieldsCorrectly() {
    given(analysisRawTransactionRepository.findMaxSourceTransactionId())
        .willReturn(Optional.empty());
    IntegratedTransactionHistory tx = buildTx(1L, 99L);
    given(
            integratedTxRepository
                .findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
                    eq(0L), any(Pageable.class)))
        .willReturn(List.of(tx));
    given(analysisRawTransactionRepository.saveAll(anyList()))
        .willAnswer(inv -> inv.getArgument(0));

    syncService.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AnalysisRawTransaction>> captor = ArgumentCaptor.forClass(List.class);
    then(analysisRawTransactionRepository).should().saveAll(captor.capture());

    AnalysisRawTransaction mapped = captor.getValue().get(0);
    assertThat(mapped.getUserId()).isEqualTo(1L);
    assertThat(mapped.getSourceType()).isEqualTo("BANK");
    assertThat(mapped.getSourceTransactionId()).isEqualTo(99L);
    assertThat(mapped.getTransactionType()).isEqualTo("INCOME");
    assertThat(mapped.getAmount()).isEqualByComparingTo(new BigDecimal("3000000"));
    assertThat(mapped.getRawPayload()).isNotBlank();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private IntegratedTransactionHistory buildTx(Long userId, Long txId) {
    IntegratedTransactionHistory tx =
        IntegratedTransactionHistory.builder()
            .userId(userId)
            .institutionType("BANK")
            .transactionType("INCOME")
            .transactionAmount(new BigDecimal("3000000"))
            .balanceAfter(new BigDecimal("5000000"))
            .originalTransactionId(txId * 2)
            .transactionOccurredAt(LocalDateTime.now())
            .syncedAt(LocalDateTime.now())
            .build();
    ReflectionTestUtils.setField(tx, "integratedTransactionId", txId);
    return tx;
  }
}
