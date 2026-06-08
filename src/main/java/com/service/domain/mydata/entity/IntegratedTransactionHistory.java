package com.service.domain.mydata.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "integrated_transaction_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class IntegratedTransactionHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "integrated_transaction_id")
  private Long integratedTransactionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "linked_account_id")
  private Long linkedAccountId;

  /** BANK / CARD */
  @Column(name = "institution_type", nullable = false, length = 30)
  private String institutionType;

  /** INCOME / EXPENSE */
  @Column(name = "transaction_type", nullable = false, length = 30)
  private String transactionType;

  @Column(name = "transaction_category", length = 100)
  private String transactionCategory;

  @Column(name = "transaction_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal transactionAmount;

  @Column(name = "balance_after", precision = 18, scale = 2)
  private BigDecimal balanceAfter;

  @Column(name = "merchant_name", length = 255)
  private String merchantName;

  @Column(name = "original_transaction_id", nullable = false)
  private Long originalTransactionId;

  @Column(name = "transaction_occurred_at", nullable = false)
  private LocalDateTime transactionOccurredAt;

  @Column(name = "synced_at", nullable = false)
  private LocalDateTime syncedAt;
}
