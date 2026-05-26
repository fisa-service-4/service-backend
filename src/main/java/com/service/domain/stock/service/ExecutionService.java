package com.service.domain.stock.service;

import com.service.domain.stock.dto.response.ExecutionListResponse;
import com.service.global.client.TransactionServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecutionService {

  private final TransactionServerClient transactionServerClient;

  public ExecutionListResponse getExecutions(
      String authorization, String stockCode, String from, String to, int page, int size) {
    return ExecutionListResponse.from(
        transactionServerClient.getExecutions(authorization, stockCode, from, to, page, size));
  }
}
