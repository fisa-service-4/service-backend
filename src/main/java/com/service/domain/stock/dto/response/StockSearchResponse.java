package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.StockItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchResponse {

  private List<StockItem> stocks;

  public static StockSearchResponse from(List<StockItem> items) {
    return StockSearchResponse.builder().stocks(items).build();
  }
}
