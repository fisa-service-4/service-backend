package com.service.domain.stock.service;

import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.mydata.entity.IntegratedStockTransactionHistory;
import com.service.domain.mydata.repository.IntegratedStockTransactionHistoryRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.domain.stock.dto.request.OrderCreateRequest;
import com.service.domain.stock.dto.response.OrderCancelResponse;
import com.service.domain.stock.dto.response.OrderDetailResponse;
import com.service.domain.stock.dto.response.OrderListResponse;
import com.service.domain.stock.dto.response.OrderResponse;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final TransactionServerClient transactionServerClient;
  private final PinAuthRepository pinAuthRepository;
  private final IntegratedStockTransactionHistoryRepository stockTransactionHistoryRepository;
  private final LinkedFinancialAccountRepository linkedFinancialAccountRepository;

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
    TransactionServerClient.OrderItem orderItem =
        transactionServerClient.createOrder(idempotencyKey, accountId, request);
    syncExecutions(userId, accountId, request.getStockCode(), orderItem);
    return OrderResponse.from(orderItem);
  }

  private void syncExecutions(
      Long userId, Long accountId, String stockCode, TransactionServerClient.OrderItem orderItem) {
    try {
      linkedFinancialAccountRepository
          .findByExternalAccountIdAndUser_UserId(accountId, userId)
          .ifPresent(
              linkedAccount -> {
                String today = LocalDate.now().toString();
                TransactionServerClient.TxPageData<TransactionServerClient.ExecutionItem>
                    executions =
                        transactionServerClient.getExecutions(
                            accountId, stockCode, today, today, 0, 100);
                if (executions == null || executions.getContent() == null) {
                  return;
                }
                List<IntegratedStockTransactionHistory> histories =
                    executions.getContent().stream()
                        .filter(e -> orderItem.getOrderId().equals(e.getOrderId()))
                        .map(
                            e ->
                                IntegratedStockTransactionHistory.builder()
                                    .userId(userId)
                                    .linkedAccountId(linkedAccount.getLinkedAccountId())
                                    .stockCode(e.getStockCode())
                                    .stockName(e.getStockName())
                                    .transactionType(orderItem.getOrderType())
                                    .transactionQuantity(e.getExecutedQuantity())
                                    .transactionUnitPrice(e.getExecutedPrice())
                                    .transactionTotalAmount(e.getExecutionAmount())
                                    .originalExecutionId(e.getExecutionId())
                                    .transactionOccurredAt(e.getExecutedAt())
                                    .syncedAt(LocalDateTime.now())
                                    .build())
                        .toList();
                if (!histories.isEmpty()) {
                  stockTransactionHistoryRepository.saveAll(histories);
                }
              });
    } catch (Exception e) {
      log.warn("주식 거래내역 동기화 실패: orderId={}, error={}", orderItem.getOrderId(), e.getMessage());
    }
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
