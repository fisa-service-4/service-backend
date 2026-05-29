package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.StockAccountItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAccountsResponse {

  private List<StockAccountItem> accounts;

  public static StockAccountsResponse from(List<StockAccountItem> accounts) {
    return StockAccountsResponse.builder().accounts(accounts).build();
  }
}
