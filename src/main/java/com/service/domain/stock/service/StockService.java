package com.service.domain.stock.service;

import com.service.domain.stock.dto.response.CashBalanceResponse;
import com.service.domain.stock.dto.response.StockAccountsResponse;
import com.service.domain.stock.dto.response.StockChartResponse;
import com.service.domain.stock.dto.response.StockPriceResponse;
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

  public StockPriceResponse getStockPrice(String authorization, String stockCode) {
    return StockPriceResponse.from(
        transactionServerClient.getStockPrice(authorization, stockCode));
  }

  public CashBalanceResponse getCashBalance(String authorization) {
    return CashBalanceResponse.from(transactionServerClient.getCashBalance(authorization));
  }

  public StockAccountsResponse getStockAccounts(String authorization) {
    return StockAccountsResponse.from(transactionServerClient.getStockAccounts(authorization));
  }

  public StockChartResponse getStockChart(
      String authorization, String stockCode, String interval, String from, String to) {
    return StockChartResponse.of(
        stockCode,
        transactionServerClient.getStockChart(authorization, stockCode, interval, from, to));
  }
}
