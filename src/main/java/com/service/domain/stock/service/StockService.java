package com.service.domain.stock.service;

import com.service.domain.stock.dto.response.CashBalanceResponse;
import com.service.domain.stock.dto.response.StockAccountsResponse;
import com.service.global.client.TransactionServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

  private final TransactionServerClient transactionServerClient;

  public CashBalanceResponse getCashBalance(Long accountId) {
    return CashBalanceResponse.from(transactionServerClient.getCashBalance(accountId));
  }

  public StockAccountsResponse getStockAccounts() {
    return StockAccountsResponse.from(transactionServerClient.getStockAccounts());
  }
}
