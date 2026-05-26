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

  public HoldingListResponse getHoldings(String authorization) {
    return HoldingListResponse.from(transactionServerClient.getHoldings(authorization));
  }

  public HoldingReturnsResponse getReturns(String authorization) {
    return HoldingReturnsResponse.from(transactionServerClient.getReturns(authorization));
  }
}
