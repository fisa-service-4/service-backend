package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.OrderDetailItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

  private Long orderId;
  private String stockCode;
  private String stockName;
  private String orderType;
  private String orderMethod;
  private Integer quantity;
  private Integer filledQuantity;
  private Integer remainingQuantity;
  private BigDecimal price;
  private BigDecimal averageExecutionPrice;
  private String status;
  private LocalDateTime orderedAt;
  private LocalDateTime updatedAt;

  public static OrderDetailResponse from(OrderDetailItem item) {
    return OrderDetailResponse.builder()
        .orderId(item.getOrderId())
        .stockCode(item.getStockCode())
        .stockName(item.getStockName())
        .orderType(item.getOrderType())
        .orderMethod(item.getOrderMethod())
        .quantity(item.getQuantity())
        .filledQuantity(item.getFilledQuantity())
        .remainingQuantity(item.getRemainingQuantity())
        .price(item.getPrice())
        .averageExecutionPrice(item.getAverageExecutionPrice())
        .status(item.getStatus())
        .orderedAt(item.getOrderedAt())
        .updatedAt(item.getUpdatedAt())
        .build();
  }
}
