package com.service.domain.analytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "analysis_asset_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisAssetSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "snapshot_id")
  private Long snapshotId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "total_asset", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAsset;

  @Column(name = "total_bank_asset", precision = 18, scale = 2)
  private BigDecimal totalBankAsset;

  @Column(name = "total_stock_asset", precision = 18, scale = 2)
  private BigDecimal totalStockAsset;

  @Column(name = "emergency_fund_amount", precision = 18, scale = 2)
  private BigDecimal emergencyFundAmount;

  @Column(name = "emergency_fund_ratio", precision = 5, scale = 2)
  private BigDecimal emergencyFundRatio;

  @Column(name = "snapshot_at", nullable = false)
  private LocalDateTime snapshotAt;
}
