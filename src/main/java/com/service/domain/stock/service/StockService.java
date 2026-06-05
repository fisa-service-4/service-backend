package com.service.domain.stock.service;

import com.service.domain.stock.dto.response.CashBalanceResponse;
import com.service.domain.stock.dto.response.StockAccountsResponse;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

  private final TransactionServerClient transactionServerClient;

  public CashBalanceResponse getCashBalance(Long accountId) {
    return CashBalanceResponse.from(transactionServerClient.getCashBalance(accountId));
  }

  public StockAccountsResponse getStockAccounts(String firebaseUid) {
    try {
      return StockAccountsResponse.from(transactionServerClient.getStockAccounts(firebaseUid));
    } catch (BusinessException e) {
      log.warn("증권 계좌 조회 실패, 빈 목록 반환: code={}", e.getErrorCode().getCode());
      return StockAccountsResponse.from(List.of());
    }
  }
}
