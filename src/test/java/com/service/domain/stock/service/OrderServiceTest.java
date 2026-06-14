package com.service.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.service.domain.auth.entity.PinAuth;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.mydata.entity.LinkedFinancialAccount;
import com.service.domain.mydata.repository.IntegratedStockTransactionHistoryRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.domain.stock.dto.request.OrderCreateRequest;
import com.service.domain.stock.dto.response.OrderCancelResponse;
import com.service.domain.stock.dto.response.OrderDetailResponse;
import com.service.domain.stock.dto.response.OrderListResponse;
import com.service.domain.stock.dto.response.OrderResponse;
import com.service.global.client.TransactionServerClient;
import com.service.global.client.TransactionServerClient.ExecutionItem;
import com.service.global.client.TransactionServerClient.OrderCancelItem;
import com.service.global.client.TransactionServerClient.OrderDetailItem;
import com.service.global.client.TransactionServerClient.OrderItem;
import com.service.global.client.TransactionServerClient.OrderListItem;
import com.service.global.client.TransactionServerClient.TxPageData;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @InjectMocks private OrderService orderService;

  @Mock private TransactionServerClient transactionServerClient;

  @Mock private PinAuthRepository pinAuthRepository;

  @Mock private IntegratedStockTransactionHistoryRepository stockTransactionHistoryRepository;

  @Mock private LinkedFinancialAccountRepository linkedFinancialAccountRepository;

  @Test
  @DisplayName("PIN이 잠기지 않은 경우 주문이 정상적으로 생성된다")
  void createOrder_createsOrderWhenPinNotLocked() {
    Long userId = 1L;
    Long accountId = 100L;
    String idempotencyKey = "key-001";

    OrderCreateRequest request = new OrderCreateRequest();
    ReflectionTestUtils.setField(request, "stockCode", "005930");
    ReflectionTestUtils.setField(request, "orderType", "BUY");
    ReflectionTestUtils.setField(request, "orderMethod", "LIMIT");
    ReflectionTestUtils.setField(request, "quantity", 5);
    ReflectionTestUtils.setField(request, "price", new BigDecimal("70000"));

    PinAuth pinAuth = PinAuth.builder().lockedYn(false).build();
    given(pinAuthRepository.findByUserId(userId)).willReturn(Optional.of(pinAuth));

    OrderItem orderItem = new OrderItem();
    ReflectionTestUtils.setField(orderItem, "orderId", 1001L);
    ReflectionTestUtils.setField(orderItem, "stockCode", "005930");
    ReflectionTestUtils.setField(orderItem, "status", "PENDING");
    given(transactionServerClient.createOrder(idempotencyKey, accountId, request))
        .willReturn(orderItem);
    given(linkedFinancialAccountRepository.findByExternalAccountIdAndUser_UserId(accountId, userId))
        .willReturn(Optional.empty());

    OrderResponse response = orderService.createOrder(userId, idempotencyKey, accountId, request);

    assertThat(response.getOrderId()).isEqualTo(1001L);
    assertThat(response.getStockCode()).isEqualTo("005930");
    then(transactionServerClient).should().createOrder(idempotencyKey, accountId, request);
  }

  @Test
  @DisplayName("PIN이 잠긴 경우 주문 생성 시 AUTH_009 예외가 발생한다")
  void createOrder_throwsExceptionWhenPinLocked() {
    Long userId = 1L;
    Long accountId = 100L;
    OrderCreateRequest request = new OrderCreateRequest();
    ReflectionTestUtils.setField(request, "stockCode", "005930");
    ReflectionTestUtils.setField(request, "orderType", "BUY");
    ReflectionTestUtils.setField(request, "orderMethod", "LIMIT");
    ReflectionTestUtils.setField(request, "quantity", 5);

    PinAuth lockedPinAuth = PinAuth.builder().lockedYn(true).build();
    given(pinAuthRepository.findByUserId(userId)).willReturn(Optional.of(lockedPinAuth));

    assertThatThrownBy(() -> orderService.createOrder(userId, "key-002", accountId, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_009);

    then(transactionServerClient).should(never()).createOrder(anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("PIN 정보가 없는 경우 주문이 정상적으로 생성된다")
  void createOrder_createsOrderWhenNoPinAuthExists() {
    Long userId = 1L;
    Long accountId = 100L;
    String idempotencyKey = "key-003";

    OrderCreateRequest request = new OrderCreateRequest();
    ReflectionTestUtils.setField(request, "stockCode", "035720");
    ReflectionTestUtils.setField(request, "orderType", "SELL");
    ReflectionTestUtils.setField(request, "orderMethod", "MARKET");
    ReflectionTestUtils.setField(request, "quantity", 3);

    given(pinAuthRepository.findByUserId(userId)).willReturn(Optional.empty());

    OrderItem orderItem = new OrderItem();
    ReflectionTestUtils.setField(orderItem, "orderId", 1002L);
    ReflectionTestUtils.setField(orderItem, "stockCode", "035720");
    ReflectionTestUtils.setField(orderItem, "status", "PENDING");
    given(transactionServerClient.createOrder(idempotencyKey, accountId, request))
        .willReturn(orderItem);
    given(linkedFinancialAccountRepository.findByExternalAccountIdAndUser_UserId(accountId, userId))
        .willReturn(Optional.empty());

    OrderResponse response = orderService.createOrder(userId, idempotencyKey, accountId, request);

    assertThat(response.getOrderId()).isEqualTo(1002L);
  }

  @Test
  @DisplayName("주문 상세 조회 시 TransactionServerClient에 위임하고 결과를 반환한다")
  void getOrderDetail_delegatesToClientAndReturnsResponse() {
    Long orderId = 1001L;
    OrderDetailItem detailItem = new OrderDetailItem();
    ReflectionTestUtils.setField(detailItem, "orderId", orderId);
    ReflectionTestUtils.setField(detailItem, "stockCode", "005930");
    ReflectionTestUtils.setField(detailItem, "status", "FILLED");
    given(transactionServerClient.getOrderDetail(orderId)).willReturn(detailItem);

    OrderDetailResponse response = orderService.getOrderDetail(orderId);

    assertThat(response.getOrderId()).isEqualTo(orderId);
    assertThat(response.getStockCode()).isEqualTo("005930");
    then(transactionServerClient).should().getOrderDetail(orderId);
  }

  @Test
  @DisplayName("주문 목록 조회 시 TransactionServerClient에 위임하고 페이지 결과를 반환한다")
  void getOrders_delegatesToClientAndReturnsPagedResponse() {
    Long accountId = 100L;
    TxPageData<OrderListItem> pageData = new TxPageData<>();
    ReflectionTestUtils.setField(pageData, "content", List.of());
    ReflectionTestUtils.setField(pageData, "page", 0);
    ReflectionTestUtils.setField(pageData, "size", 10);
    ReflectionTestUtils.setField(pageData, "totalElements", 0L);
    ReflectionTestUtils.setField(pageData, "totalPages", 0);
    given(transactionServerClient.getOrders(accountId, "ALL", "ALL", 0, 10)).willReturn(pageData);

    OrderListResponse response = orderService.getOrders(accountId, "ALL", "ALL", 0, 10);

    assertThat(response.getContent()).isEmpty();
    then(transactionServerClient).should().getOrders(accountId, "ALL", "ALL", 0, 10);
  }

  @Test
  @DisplayName("PIN이 잠기지 않은 경우 주문 취소가 정상적으로 처리된다")
  void cancelOrder_cancelsOrderWhenPinNotLocked() {
    Long userId = 1L;
    Long orderId = 1001L;
    String idempotencyKey = "cancel-key-001";

    PinAuth pinAuth = PinAuth.builder().lockedYn(false).build();
    given(pinAuthRepository.findByUserId(userId)).willReturn(Optional.of(pinAuth));

    OrderCancelItem cancelItem = new OrderCancelItem();
    ReflectionTestUtils.setField(cancelItem, "orderId", orderId);
    ReflectionTestUtils.setField(cancelItem, "status", "CANCELLED");
    ReflectionTestUtils.setField(cancelItem, "cancelledQuantity", 5);
    given(transactionServerClient.cancelOrder(idempotencyKey, orderId)).willReturn(cancelItem);

    OrderCancelResponse response = orderService.cancelOrder(userId, idempotencyKey, orderId);

    assertThat(response.getOrderId()).isEqualTo(orderId);
    assertThat(response.getStatus()).isEqualTo("CANCELLED");
    then(transactionServerClient).should().cancelOrder(idempotencyKey, orderId);
  }

  @Test
  @DisplayName("PIN이 잠긴 경우 주문 취소 시 AUTH_009 예외가 발생한다")
  void cancelOrder_throwsExceptionWhenPinLocked() {
    Long userId = 1L;
    Long orderId = 1001L;

    PinAuth lockedPinAuth = PinAuth.builder().lockedYn(true).build();
    given(pinAuthRepository.findByUserId(userId)).willReturn(Optional.of(lockedPinAuth));

    assertThatThrownBy(() -> orderService.cancelOrder(userId, "cancel-key-002", orderId))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_009);

    then(transactionServerClient).should(never()).cancelOrder(anyString(), anyLong());
  }

  @Test
  @DisplayName("연동 계좌가 존재하고 체결 내역이 있을 때 거래내역이 로컬 DB에 저장된다")
  void createOrder_savesExecutionHistoryWhenLinkedAccountExists() {
    Long userId = 1L;
    Long accountId = 100L;
    Long orderId = 1004L;
    String idempotencyKey = "key-005";

    OrderCreateRequest request = new OrderCreateRequest();
    ReflectionTestUtils.setField(request, "stockCode", "005930");
    ReflectionTestUtils.setField(request, "orderType", "BUY");
    ReflectionTestUtils.setField(request, "orderMethod", "LIMIT");
    ReflectionTestUtils.setField(request, "quantity", 3);
    ReflectionTestUtils.setField(request, "price", new BigDecimal("70000"));

    given(pinAuthRepository.findByUserId(userId)).willReturn(Optional.empty());

    OrderItem orderItem = new OrderItem();
    ReflectionTestUtils.setField(orderItem, "orderId", orderId);
    ReflectionTestUtils.setField(orderItem, "stockCode", "005930");
    ReflectionTestUtils.setField(orderItem, "orderType", "BUY");
    ReflectionTestUtils.setField(orderItem, "status", "PENDING");
    given(transactionServerClient.createOrder(idempotencyKey, accountId, request))
        .willReturn(orderItem);

    LinkedFinancialAccount linkedAccount =
        LinkedFinancialAccount.builder().linkedAccountId(10L).build();
    given(linkedFinancialAccountRepository.findByExternalAccountIdAndUser_UserId(accountId, userId))
        .willReturn(Optional.of(linkedAccount));

    ExecutionItem executionItem = new ExecutionItem();
    ReflectionTestUtils.setField(executionItem, "executionId", 500L);
    ReflectionTestUtils.setField(executionItem, "orderId", orderId);
    ReflectionTestUtils.setField(executionItem, "stockCode", "005930");
    ReflectionTestUtils.setField(executionItem, "stockName", "삼성전자");
    ReflectionTestUtils.setField(executionItem, "executedPrice", new BigDecimal("70000"));
    ReflectionTestUtils.setField(executionItem, "executedQuantity", 3);
    ReflectionTestUtils.setField(executionItem, "executionAmount", new BigDecimal("210000"));
    ReflectionTestUtils.setField(executionItem, "executedAt", LocalDateTime.now());

    TxPageData<ExecutionItem> executionsPage = new TxPageData<>();
    ReflectionTestUtils.setField(executionsPage, "content", List.of(executionItem));
    given(
            transactionServerClient.getExecutions(
                anyLong(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
        .willReturn(executionsPage);

    OrderResponse response = orderService.createOrder(userId, idempotencyKey, accountId, request);

    assertThat(response.getOrderId()).isEqualTo(orderId);
    then(stockTransactionHistoryRepository).should().saveAll(any());
  }

  @Test
  @DisplayName("거래내역 동기화 중 예외 발생 시 주문은 정상적으로 반환된다")
  void createOrder_returnSuccessEvenWhenSyncFails() {
    Long userId = 1L;
    Long accountId = 100L;
    String idempotencyKey = "key-004";

    OrderCreateRequest request = new OrderCreateRequest();
    ReflectionTestUtils.setField(request, "stockCode", "005930");
    ReflectionTestUtils.setField(request, "orderType", "BUY");
    ReflectionTestUtils.setField(request, "orderMethod", "LIMIT");
    ReflectionTestUtils.setField(request, "quantity", 2);
    ReflectionTestUtils.setField(request, "price", new BigDecimal("70000"));

    given(pinAuthRepository.findByUserId(userId)).willReturn(Optional.empty());

    OrderItem orderItem = new OrderItem();
    ReflectionTestUtils.setField(orderItem, "orderId", 1003L);
    ReflectionTestUtils.setField(orderItem, "stockCode", "005930");
    ReflectionTestUtils.setField(orderItem, "status", "PENDING");
    given(transactionServerClient.createOrder(idempotencyKey, accountId, request))
        .willReturn(orderItem);

    given(linkedFinancialAccountRepository.findByExternalAccountIdAndUser_UserId(accountId, userId))
        .willThrow(new RuntimeException("DB 연결 오류"));

    OrderResponse response = orderService.createOrder(userId, idempotencyKey, accountId, request);

    assertThat(response.getOrderId()).isEqualTo(1003L);
  }
}
