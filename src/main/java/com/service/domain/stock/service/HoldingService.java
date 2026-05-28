package com.service.domain.stock.service;

import com.service.domain.stock.dto.response.HoldingListResponse;
import com.service.domain.stock.dto.response.HoldingReturnsResponse;
import com.service.global.client.TransactionServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HoldingService {

  private final TransactionServerClient transactionServerClient;

  public HoldingListResponse getHoldings(Long accountId) {
    return HoldingListResponse.from(transactionServerClient.getHoldings(accountId));
  }

  public HoldingReturnsResponse getReturns(Long accountId) {
    return HoldingReturnsResponse.from(transactionServerClient.getReturns(accountId));
  }
}
