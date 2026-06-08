package com.service.domain.mydata.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "integrated_stock_transaction_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class IntegratedStockTransactionHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "integrated_stock_transaction_id")
  private Long integratedStockTransactionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "linked_account_id", nullable = false)
  private Long linkedAccountId;

  @Column(name = "stock_code", nullable = false, length = 20)
  private String stockCode;

  @Column(name = "stock_name", nullable = false, length = 255)
  private String stockName;

  /** BUY / SELL */
  @Column(name = "transaction_type", nullable = false, length = 30)
  private String transactionType;

  @Column(name = "transaction_quantity", nullable = false)
  private Integer transactionQuantity;

  @Column(name = "transaction_unit_price", nullable = false, precision = 18, scale = 2)
  private BigDecimal transactionUnitPrice;

  @Column(name = "transaction_total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal transactionTotalAmount;

  /** 거래 후 증권 계좌 예수금 */
  @Column(name = "cash_balance_after", precision = 18, scale = 2)
  private BigDecimal cashBalanceAfter;

  @Column(name = "holding_quantity_after")
  private Integer holdingQuantityAfter;

  @Column(name = "average_purchase_price", precision = 18, scale = 2)
  private BigDecimal averagePurchasePrice;

  @Column(name = "realized_profit", precision = 18, scale = 2)
  private BigDecimal realizedProfit;

  @Column(name = "realized_profit_rate", precision = 5, scale = 2)
  private BigDecimal realizedProfitRate;

  @Column(name = "original_execution_id", nullable = false)
  private Long originalExecutionId;

  @Column(name = "transaction_occurred_at", nullable = false)
  private LocalDateTime transactionOccurredAt;

  @Column(name = "synced_at", nullable = false)
  private LocalDateTime syncedAt;
}
