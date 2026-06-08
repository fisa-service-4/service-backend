package com.service.domain.analytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "analysis_raw_transaction",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_analysis_raw_tx_source",
            columnNames = {"source_type", "source_transaction_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisRawTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long rawTransactionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** 데이터 출처 (BANK, CARD) */
  @Column(name = "source_type", nullable = false, length = 30)
  private String sourceType;

  /** 운영 DB integrated_transaction_history.integrated_transaction_id */
  @Column(name = "source_transaction_id", nullable = false)
  private Long sourceTransactionId;

  @Column(name = "account_id")
  private Long accountId;

  @Column(name = "transaction_type", nullable = false, length = 50)
  private String transactionType;

  @Column(name = "category", length = 100)
  private String category;

  @Column(name = "amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "balance_after", precision = 18, scale = 2)
  private BigDecimal balanceAfter;

  @Column(name = "transaction_at", nullable = false)
  private LocalDateTime transactionAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
  private String rawPayload;

  @Column(name = "synced_at", nullable = false)
  private LocalDateTime syncedAt;
}
