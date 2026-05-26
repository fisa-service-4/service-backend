package com.service.domain.stock.service;

import com.service.domain.stock.dto.response.PortfolioResponse;
import com.service.global.client.TransactionServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioService {

  private final TransactionServerClient transactionServerClient;

  public PortfolioResponse getPortfolio(String authorization) {
    return PortfolioResponse.from(transactionServerClient.getPortfolio(authorization));
  }
}
