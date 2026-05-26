package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.OrderListItem;
import com.service.global.client.TransactionServerClient.TxPageData;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {

  private List<OrderListItem> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;

  public static OrderListResponse from(TxPageData<OrderListItem> data) {
    return OrderListResponse.builder()
        .content(data.getContent())
        .page(data.getPage())
        .size(data.getSize())
        .totalElements(data.getTotalElements())
        .totalPages(data.getTotalPages())
        .build();
  }
}
