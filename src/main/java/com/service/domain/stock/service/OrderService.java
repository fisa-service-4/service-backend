package com.service.domain.stock.service;

import com.service.domain.stock.dto.request.OrderCreateRequest;
import com.service.domain.stock.dto.response.OrderResponse;
import com.service.global.client.TransactionServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final TransactionServerClient transactionServerClient;

  public OrderResponse createOrder(
      String authorization, String pinToken, String idempotencyKey,
      OrderCreateRequest request) {
    return OrderResponse.from(
        transactionServerClient.createOrder(authorization, pinToken, idempotencyKey, request));
  }
}
