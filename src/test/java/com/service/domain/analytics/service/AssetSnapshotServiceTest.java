package com.service.domain.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.service.domain.analytics.entity.AnalysisAssetSnapshot;
import com.service.domain.analytics.repository.AnalysisAssetSnapshotRepository;
import com.service.domain.mydata.entity.IntegratedStockTransactionHistory;
import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import com.service.domain.mydata.repository.IntegratedStockTransactionHistoryRepository;
import com.service.domain.mydata.repository.IntegratedTransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssetSnapshotServiceTest {

  @InjectMocks private AssetSnapshotService assetSnapshotService;

  @Mock private IntegratedTransactionHistoryRepository integratedTxRepository;

  @Mock private IntegratedStockTransactionHistoryRepository integratedStockTxRepository;

  @Mock private AnalysisAssetSnapshotRepository assetSnapshotRepository;

  @Test
  @DisplayName("거래내역 보유 사용자가 없으면 스냅샷을 저장하지 않는다")
  void createSnapshots_skipsWhenNoUsers() {
    given(integratedTxRepository.findAllDistinctUserIds()).willReturn(Collections.emptyList());
    given(integratedStockTxRepository.findAllDistinctUserIds()).willReturn(Collections.emptyList());

    assetSnapshotService.createSnapshots();

    then(assetSnapshotRepository).should(never()).save(any(AnalysisAssetSnapshot.class));
  }

  @Test
  @DisplayName("은행 거래내역이 있는 사용자에 대해 스냅샷이 저장된다")
  void createSnapshots_savesSnapshotForBankUser() {
    Long userId = 1L;
    given(integratedTxRepository.findAllDistinctUserIds()).willReturn(List.of(userId));
    given(integratedStockTxRepository.findAllDistinctUserIds()).willReturn(Collections.emptyList());

    IntegratedTransactionHistory bankTx = buildBankTx(userId, new BigDecimal("3500000"));
    given(integratedTxRepository.findLatestByUserIdsAndInstitutionType(List.of(userId), "BANK"))
        .willReturn(List.of(bankTx));
    given(integratedStockTxRepository.findLatestByUserIds(List.of(userId)))
        .willReturn(Collections.emptyList());

    assetSnapshotService.createSnapshots();

    ArgumentCaptor<AnalysisAssetSnapshot> captor =
        ArgumentCaptor.forClass(AnalysisAssetSnapshot.class);
    then(assetSnapshotRepository).should().save(captor.capture());
    AnalysisAssetSnapshot saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getTotalBankAsset()).isEqualByComparingTo(new BigDecimal("3500000"));
    assertThat(saved.getTotalStockAsset()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(saved.getTotalAsset()).isEqualByComparingTo(new BigDecimal("3500000"));
  }

  @Test
  @DisplayName("증권 거래내역이 있는 사용자에 대해 스냅샷이 저장된다")
  void createSnapshots_savesSnapshotForStockUser() {
    Long userId = 2L;
    given(integratedTxRepository.findAllDistinctUserIds()).willReturn(Collections.emptyList());
    given(integratedStockTxRepository.findAllDistinctUserIds()).willReturn(List.of(userId));

    given(integratedTxRepository.findLatestByUserIdsAndInstitutionType(List.of(userId), "BANK"))
        .willReturn(Collections.emptyList());
    IntegratedStockTransactionHistory stockTx = buildStockTx(userId, new BigDecimal("15000000"));
    given(integratedStockTxRepository.findLatestByUserIds(List.of(userId)))
        .willReturn(List.of(stockTx));

    assetSnapshotService.createSnapshots();

    ArgumentCaptor<AnalysisAssetSnapshot> captor =
        ArgumentCaptor.forClass(AnalysisAssetSnapshot.class);
    then(assetSnapshotRepository).should().save(captor.capture());
    AnalysisAssetSnapshot saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getTotalBankAsset()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(saved.getTotalStockAsset()).isEqualByComparingTo(new BigDecimal("15000000"));
    assertThat(saved.getTotalAsset()).isEqualByComparingTo(new BigDecimal("15000000"));
  }

  @Test
  @DisplayName("은행과 증권 자산이 합산되어 totalAsset이 계산된다")
  void createSnapshots_sumsBankAndStockAssets() {
    Long userId = 1L;
    given(integratedTxRepository.findAllDistinctUserIds()).willReturn(List.of(userId));
    given(integratedStockTxRepository.findAllDistinctUserIds()).willReturn(List.of(userId));

    IntegratedTransactionHistory bankTx = buildBankTx(userId, new BigDecimal("3000000"));
    IntegratedStockTransactionHistory stockTx = buildStockTx(userId, new BigDecimal("15000000"));
    given(integratedTxRepository.findLatestByUserIdsAndInstitutionType(List.of(userId), "BANK"))
        .willReturn(List.of(bankTx));
    given(integratedStockTxRepository.findLatestByUserIds(List.of(userId)))
        .willReturn(List.of(stockTx));

    assetSnapshotService.createSnapshots();

    ArgumentCaptor<AnalysisAssetSnapshot> captor =
        ArgumentCaptor.forClass(AnalysisAssetSnapshot.class);
    then(assetSnapshotRepository).should().save(captor.capture());
    AnalysisAssetSnapshot saved = captor.getValue();
    assertThat(saved.getTotalAsset()).isEqualByComparingTo(new BigDecimal("18000000"));
    assertThat(saved.getTotalBankAsset()).isEqualByComparingTo(new BigDecimal("3000000"));
    assertThat(saved.getTotalStockAsset()).isEqualByComparingTo(new BigDecimal("15000000"));
  }

  @Test
  @DisplayName("특정 사용자의 스냅샷 저장이 실패해도 다른 사용자의 처리는 계속된다")
  void createSnapshots_continuesWhenOneUserFails() {
    Long userId1 = 1L;
    Long userId2 = 2L;
    given(integratedTxRepository.findAllDistinctUserIds()).willReturn(List.of(userId1, userId2));
    given(integratedStockTxRepository.findAllDistinctUserIds()).willReturn(Collections.emptyList());

    IntegratedTransactionHistory bankTx1 = buildBankTx(userId1, new BigDecimal("3000000"));
    IntegratedTransactionHistory bankTx2 = buildBankTx(userId2, new BigDecimal("5000000"));
    // HashSet으로 합산된 userIds 순서가 비결정적이므로 anyList() 사용
    given(integratedTxRepository.findLatestByUserIdsAndInstitutionType(anyList(), eq("BANK")))
        .willReturn(List.of(bankTx1, bankTx2));
    given(integratedStockTxRepository.findLatestByUserIds(anyList()))
        .willReturn(Collections.emptyList());

    given(assetSnapshotRepository.save(any()))
        .willThrow(new RuntimeException("DB 오류"))
        .willAnswer(inv -> inv.getArgument(0));

    assetSnapshotService.createSnapshots();

    // 두 사용자 모두에 대한 save 시도가 이루어졌음을 확인
    then(assetSnapshotRepository).should(org.mockito.Mockito.times(2)).save(any());
  }

  @Test
  @DisplayName("balanceAfter가 null인 은행 거래내역은 자산 계산에서 0으로 처리된다")
  void createSnapshots_treatsNullBalanceAsZero() {
    Long userId = 1L;
    given(integratedTxRepository.findAllDistinctUserIds()).willReturn(List.of(userId));
    given(integratedStockTxRepository.findAllDistinctUserIds()).willReturn(Collections.emptyList());

    IntegratedTransactionHistory txWithNullBalance = buildBankTxWithNullBalance(userId);
    given(integratedTxRepository.findLatestByUserIdsAndInstitutionType(List.of(userId), "BANK"))
        .willReturn(List.of(txWithNullBalance));
    given(integratedStockTxRepository.findLatestByUserIds(List.of(userId)))
        .willReturn(Collections.emptyList());

    assetSnapshotService.createSnapshots();

    ArgumentCaptor<AnalysisAssetSnapshot> captor =
        ArgumentCaptor.forClass(AnalysisAssetSnapshot.class);
    then(assetSnapshotRepository).should().save(captor.capture());
    assertThat(captor.getValue().getTotalBankAsset()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private IntegratedTransactionHistory buildBankTx(Long userId, BigDecimal balanceAfter) {
    IntegratedTransactionHistory tx =
        IntegratedTransactionHistory.builder()
            .userId(userId)
            .institutionType("BANK")
            .transactionType("INCOME")
            .transactionAmount(new BigDecimal("1000000"))
            .balanceAfter(balanceAfter)
            .originalTransactionId(100L)
            .transactionOccurredAt(LocalDateTime.now())
            .syncedAt(LocalDateTime.now())
            .build();
    ReflectionTestUtils.setField(tx, "integratedTransactionId", userId * 10);
    return tx;
  }

  private IntegratedTransactionHistory buildBankTxWithNullBalance(Long userId) {
    IntegratedTransactionHistory tx =
        IntegratedTransactionHistory.builder()
            .userId(userId)
            .institutionType("BANK")
            .transactionType("INCOME")
            .transactionAmount(new BigDecimal("1000000"))
            .balanceAfter(null)
            .originalTransactionId(100L)
            .transactionOccurredAt(LocalDateTime.now())
            .syncedAt(LocalDateTime.now())
            .build();
    ReflectionTestUtils.setField(tx, "integratedTransactionId", userId * 10);
    return tx;
  }

  private IntegratedStockTransactionHistory buildStockTx(Long userId, BigDecimal cashBalanceAfter) {
    return IntegratedStockTransactionHistory.builder()
        .userId(userId)
        .linkedAccountId(200L)
        .stockCode("005930")
        .stockName("삼성전자")
        .transactionType("BUY")
        .transactionQuantity(10)
        .transactionUnitPrice(new BigDecimal("82000"))
        .transactionTotalAmount(new BigDecimal("820000"))
        .cashBalanceAfter(cashBalanceAfter)
        .originalExecutionId(300L)
        .transactionOccurredAt(LocalDateTime.now())
        .syncedAt(LocalDateTime.now())
        .build();
  }
}
