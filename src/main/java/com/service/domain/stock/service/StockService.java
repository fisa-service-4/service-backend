package com.service.domain.stock.service;

import com.service.domain.stock.dto.response.StockSearchResponse;
import com.service.global.client.TransactionServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

  private final TransactionServerClient transactionServerClient;

  public StockSearchResponse searchStocks(String authorization, String keyword) {
    return StockSearchResponse.from(
        transactionServerClient.searchStocks(authorization, keyword));
  }
}
