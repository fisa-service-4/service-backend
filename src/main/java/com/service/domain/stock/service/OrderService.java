package com.service.domain.stock.service;

import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.stock.dto.request.OrderCreateRequest;
import com.service.domain.stock.dto.response.OrderCancelResponse;
import com.service.domain.stock.dto.response.OrderDetailResponse;
import com.service.domain.stock.dto.response.OrderListResponse;
import com.service.domain.stock.dto.response.OrderResponse;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final TransactionServerClient transactionServerClient;
  private final PinAuthRepository pinAuthRepository;

  private void checkPinLocked(Long userId) {
    pinAuthRepository
        .findByUserId(userId)
        .ifPresent(
            pinAuth -> {
              if (Boolean.TRUE.equals(pinAuth.getLockedYn())) {
                throw new BusinessException(ErrorCode.AUTH_009);
              }
            });
  }

  public OrderResponse createOrder(
      Long userId, String idempotencyKey, Long accountId, OrderCreateRequest request) {
    checkPinLocked(userId);
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

  public OrderCancelResponse cancelOrder(Long userId, String idempotencyKey, Long orderId) {
    checkPinLocked(userId);
    return OrderCancelResponse.from(transactionServerClient.cancelOrder(idempotencyKey, orderId));
  }
}
