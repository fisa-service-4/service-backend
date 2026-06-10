package com.service.domain.admin.dto.response;

import com.service.domain.mydata.entity.IntegratedStockTransactionHistory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStockOrderLogResponse {

  private String buyerName;
  private String accountNumberMasked;
  private String transactionType;
  private Integer quantity;
  private BigDecimal totalAmount;
  private BigDecimal cashBalanceAfter;
  private LocalDateTime transactionOccurredAt;

  public static AdminStockOrderLogResponse of(
      IntegratedStockTransactionHistory history, String buyerName, String accountNumberMasked) {
    return AdminStockOrderLogResponse.builder()
        .buyerName(buyerName)
        .accountNumberMasked(accountNumberMasked)
        .transactionType(history.getTransactionType())
        .quantity(history.getTransactionQuantity())
        .totalAmount(history.getTransactionTotalAmount())
        .cashBalanceAfter(history.getCashBalanceAfter())
        .transactionOccurredAt(history.getTransactionOccurredAt())
        .build();
  }
}
