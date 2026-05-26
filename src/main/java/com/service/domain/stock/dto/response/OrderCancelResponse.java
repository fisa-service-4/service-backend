package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.OrderCancelItem;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelResponse {

  private Long orderId;
  private String status;
  private Integer cancelledQuantity;
  private Integer filledQuantity;
  private Integer remainingQuantity;
  private LocalDateTime cancelledAt;

  public static OrderCancelResponse from(OrderCancelItem item) {
    return OrderCancelResponse.builder()
        .orderId(item.getOrderId())
        .status(item.getStatus())
        .cancelledQuantity(item.getCancelledQuantity())
        .filledQuantity(item.getFilledQuantity())
        .remainingQuantity(item.getRemainingQuantity())
        .cancelledAt(item.getCancelledAt())
        .build();
  }
}
