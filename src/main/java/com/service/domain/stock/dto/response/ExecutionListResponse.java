package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.ExecutionItem;
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
public class ExecutionListResponse {

  private List<ExecutionItem> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;

  public static ExecutionListResponse from(TxPageData<ExecutionItem> data) {
    return ExecutionListResponse.builder()
        .content(data.getContent())
        .page(data.getPage())
        .size(data.getSize())
        .totalElements(data.getTotalElements())
        .totalPages(data.getTotalPages())
        .build();
  }
}
