package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.OrderItem;
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
public class OrderResponse {

  private Long orderId;
  private String stockCode;
  private String orderType;
  private String orderMethod;
  private Integer quantity;
  private BigDecimal price;
  private Integer filledQuantity;
  private Integer remainingQuantity;
  private String status;
  private LocalDateTime orderedAt;

  public static OrderResponse from(OrderItem item) {
    return OrderResponse.builder()
        .orderId(item.getOrderId())
        .stockCode(item.getStockCode())
        .orderType(item.getOrderType())
        .orderMethod(item.getOrderMethod())
        .quantity(item.getQuantity())
        .price(item.getPrice())
        .filledQuantity(item.getFilledQuantity())
        .remainingQuantity(item.getRemainingQuantity())
        .status(item.getStatus())
        .orderedAt(item.getOrderedAt())
        .build();
  }
}
