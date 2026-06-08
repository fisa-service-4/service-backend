package com.service.domain.analytics.service;

import com.service.domain.analytics.entity.AnalysisAssetSnapshot;
import com.service.domain.analytics.repository.AnalysisAssetSnapshotRepository;
import com.service.domain.mydata.entity.IntegratedStockTransactionHistory;
import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import com.service.domain.mydata.repository.IntegratedStockTransactionHistoryRepository;
import com.service.domain.mydata.repository.IntegratedTransactionHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetSnapshotService {

  private final IntegratedTransactionHistoryRepository integratedTxRepository;
  private final IntegratedStockTransactionHistoryRepository integratedStockTxRepository;
  private final AnalysisAssetSnapshotRepository assetSnapshotRepository;

  /** 거래내역이 존재하는 모든 사용자의 자산 스냅샷을 생성한다. */
  public void createSnapshots() {
    List<Long> userIds = collectUserIds();
    log.info("자산 스냅샷 생성 대상 사용자 수: {}", userIds.size());

    int successCount = 0;
    for (Long userId : userIds) {
      try {
        createSnapshotForUser(userId);
        successCount++;
      } catch (Exception e) {
        log.error("사용자 {} 자산 스냅샷 생성 실패: {}", userId, e.getMessage());
      }
    }
    log.info("자산 스냅샷 생성 완료: {}/{}건", successCount, userIds.size());
  }

  private List<Long> collectUserIds() {
    Set<Long> ids = new HashSet<>();
    ids.addAll(integratedTxRepository.findAllDistinctUserIds());
    ids.addAll(integratedStockTxRepository.findAllDistinctUserIds());
    return List.copyOf(ids);
  }

  private void createSnapshotForUser(Long userId) {
    BigDecimal bankAsset = resolveBankAsset(userId);
    BigDecimal stockAsset = resolveStockAsset(userId);
    BigDecimal totalAsset = bankAsset.add(stockAsset);

    BigDecimal emergencyRatio =
        totalAsset.compareTo(BigDecimal.ZERO) > 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : null;

    AnalysisAssetSnapshot snapshot =
        AnalysisAssetSnapshot.builder()
            .userId(userId)
            .totalAsset(totalAsset)
            .totalBankAsset(bankAsset)
            .totalStockAsset(stockAsset)
            .emergencyFundAmount(null)
            .emergencyFundRatio(emergencyRatio)
            .snapshotAt(LocalDateTime.now())
            .build();

    assetSnapshotRepository.save(snapshot);
    log.debug(
        "사용자 {} 자산 스냅샷 저장 (bank={}, stock={}, total={})",
        userId,
        bankAsset,
        stockAsset,
        totalAsset);
  }

  private BigDecimal resolveBankAsset(Long userId) {
    return integratedTxRepository
        .findFirstByUserIdAndInstitutionTypeOrderByTransactionOccurredAtDesc(userId, "BANK")
        .map(IntegratedTransactionHistory::getBalanceAfter)
        .filter(b -> b != null)
        .orElse(BigDecimal.ZERO);
  }

  private BigDecimal resolveStockAsset(Long userId) {
    return integratedStockTxRepository
        .findFirstByUserIdOrderByTransactionOccurredAtDesc(userId)
        .map(IntegratedStockTransactionHistory::getCashBalanceAfter)
        .filter(b -> b != null)
        .orElse(BigDecimal.ZERO);
  }
}
