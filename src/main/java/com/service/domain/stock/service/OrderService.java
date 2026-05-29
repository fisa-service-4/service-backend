package com.service.domain.stock.service;

import com.service.domain.stock.dto.request.OrderCreateRequest;
import com.service.domain.stock.dto.response.OrderCancelResponse;
import com.service.domain.stock.dto.response.OrderDetailResponse;
import com.service.domain.stock.dto.response.OrderListResponse;
import com.service.domain.stock.dto.response.OrderResponse;
import com.service.global.client.TransactionServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final TransactionServerClient transactionServerClient;

  public OrderResponse createOrder(
      String idempotencyKey, Long accountId, OrderCreateRequest request) {
    return OrderResponse.from(
        transactionServerClient.createOrder(idempotencyKey, accountId, request));
  }

  public OrderDetailResponse getOrderDetail(Long orderId) {
    return OrderDetailResponse.from(transactionServerClient.getOrderDetail(orderId));
  }

  public OrderListResponse getOrders(
      Long accountId, String status, String orderType, int page, int size) {
    return OrderListResponse.from(
        transactionServerClient.getOrders(accountId, status, orderType, page, size));
  }

  public OrderCancelResponse cancelOrder(String idempotencyKey, Long orderId) {
    return OrderCancelResponse.from(transactionServerClient.cancelOrder(idempotencyKey, orderId));
  }
}
