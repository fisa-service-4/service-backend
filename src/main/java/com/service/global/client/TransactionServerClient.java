package com.service.global.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TransactionServerClient {

  private final RestClient transactionServerRestClient;

  // ───────────────────────────────────────────────
  // 공통 내부 타입
  // ───────────────────────────────────────────────

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TxResponse<T> {
    private boolean success;
    private T data;
    private TxError error;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TxError {
    private String code;
    private String message;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TxContentData<T> {
    private List<T> content;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TxPageData<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
  }

  // ───────────────────────────────────────────────
  // 수익률 조회
  // ───────────────────────────────────────────────

  public ReturnsItem getReturns(Long accountId) {
    try {
      TxResponse<ReturnsItem> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stock/accounts/{accountId}/returns", accountId)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 보유 종목 조회
  // ───────────────────────────────────────────────

  public List<HoldingItem> getHoldings(Long accountId) {
    try {
      TxResponse<TxContentData<HoldingItem>> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stock/accounts/{accountId}/holdings", accountId)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData().getContent();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 체결 내역 조회
  // ───────────────────────────────────────────────

  public TxPageData<ExecutionItem> getExecutions(
      Long accountId, String stockCode, String from, String to, int page, int size) {
    try {
      TxResponse<TxPageData<ExecutionItem>> response =
          transactionServerRestClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/baas/v1/stock/accounts/{accountId}/executions")
                          .queryParamIfPresent(
                              "stockCode", java.util.Optional.ofNullable(stockCode))
                          .queryParamIfPresent("fromDate", java.util.Optional.ofNullable(from))
                          .queryParamIfPresent("toDate", java.util.Optional.ofNullable(to))
                          .queryParam("page", page)
                          .queryParam("size", size)
                          .build(accountId))
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 주문 상세 조회
  // ───────────────────────────────────────────────

  public OrderDetailItem getOrderDetail(Long orderId) {
    try {
      TxResponse<OrderDetailItem> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stock/orders/{orderId}", orderId)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 주문 내역 조회
  // ───────────────────────────────────────────────

  public TxPageData<OrderListItem> getOrders(
      Long accountId, String status, String orderType, int page, int size) {
    try {
      TxResponse<TxPageData<OrderListItem>> response =
          transactionServerRestClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/baas/v1/stock/accounts/{accountId}/orders")
                          .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
                          .queryParamIfPresent(
                              "orderType", java.util.Optional.ofNullable(orderType))
                          .queryParam("page", page)
                          .queryParam("size", size)
                          .build(accountId))
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 주문 취소
  // ───────────────────────────────────────────────

  public OrderCancelItem cancelOrder(String idempotencyKey, Long orderId) {
    try {
      TxResponse<OrderCancelItem> response =
          transactionServerRestClient
              .post()
              .uri("/baas/v1/stock/orders/{orderId}/cancel", orderId)
              .header("Idempotency-Key", idempotencyKey)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 주문 생성
  // ───────────────────────────────────────────────

  public OrderItem createOrder(
      String idempotencyKey,
      Long accountId,
      com.service.domain.stock.dto.request.OrderCreateRequest request) {
    try {
      TxResponse<OrderItem> response =
          transactionServerRestClient
              .post()
              .uri("/baas/v1/stock/accounts/{accountId}/orders", accountId)
              .header("Idempotency-Key", idempotencyKey)
              .body(request)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 예수금 조회
  // ───────────────────────────────────────────────

  public CashBalanceItem getCashBalance(Long accountId) {
    try {
      TxResponse<CashBalanceItem> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stock/accounts/{accountId}/cash-balance", accountId)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 주문 가능 계좌 조회
  // ───────────────────────────────────────────────

  public List<StockAccountItem> getStockAccounts(String firebaseUid) {
    try {
      TxResponse<TxContentData<StockAccountItem>> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stock/accounts")
              .header("x-firebase-uid", firebaseUid)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData().getContent();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 사용자 연동
  // ───────────────────────────────────────────────

  public void linkUser(String firebaseUid, String name, String phoneNumber) {
    try {
      transactionServerRestClient
          .post()
          .uri("/baas/v1/user/link")
          .body(new LinkUserRequest(firebaseUid, name, phoneNumber))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  public record LinkUserRequest(String firebaseUid, String name, String phoneNumber) {}

  // ───────────────────────────────────────────────
  // 현재가 조회 (관심종목 내부 사용)
  // ───────────────────────────────────────────────

  public StockPriceItem getStockPrice(String stockCode) {
    try {
      TxResponse<StockPriceItem> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stock/{stockCode}/price", stockCode)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 종목 관련 내부 타입
  // ───────────────────────────────────────────────

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StockPriceItem {
    private String stockCode;
    private String stockName;
    private java.math.BigDecimal currentPrice;
    private java.math.BigDecimal changeRate;
    private java.time.LocalDateTime updatedAt;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StockAccountItem {
    private Long accountId;
    private String accountNumber;
    private String accountName;
    private String bankCode;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CashBalanceItem {
    private java.math.BigDecimal cashBalance;
    private java.math.BigDecimal availableBalance;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OrderItem {
    private Long orderId;
    private String stockCode;
    private String orderType;
    private String orderMethod;
    private Integer quantity;
    private java.math.BigDecimal price;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private String status;
    private java.time.LocalDateTime orderedAt;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OrderCancelItem {
    private Long orderId;
    private String status;
    private Integer cancelledQuantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private java.time.LocalDateTime cancelledAt;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OrderListItem {
    private Long orderId;
    private String stockCode;
    private String stockName;
    private String orderType;
    private String orderMethod;
    private Integer quantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private java.math.BigDecimal price;
    private String status;
    private java.time.LocalDateTime orderedAt;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ReturnsItem {
    private java.math.BigDecimal dailyReturnRate;
    private java.math.BigDecimal monthlyReturnRate;
    private java.math.BigDecimal yearlyReturnRate;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class HoldingItem {
    private String stockCode;
    private String stockName;
    private Integer quantity;
    private java.math.BigDecimal averagePrice;
    private java.math.BigDecimal currentPrice;
    private java.math.BigDecimal evaluationAmount;
    private java.math.BigDecimal unrealizedProfit;
    private java.math.BigDecimal profitRate;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ExecutionItem {
    private Long executionId;
    private Long orderId;
    private String stockCode;
    private String stockName;
    private java.math.BigDecimal executedPrice;
    private Integer executedQuantity;
    private java.math.BigDecimal executionAmount;
    private java.time.LocalDateTime executedAt;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OrderDetailItem {
    private Long orderId;
    private String stockCode;
    private String stockName;
    private String orderType;
    private String orderMethod;
    private Integer quantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private java.math.BigDecimal price;
    private java.math.BigDecimal averageExecutionPrice;
    private String status;
    private java.time.LocalDateTime orderedAt;
    private java.time.LocalDateTime updatedAt;
  }

  // ───────────────────────────────────────────────
  // 에러 처리
  // ───────────────────────────────────────────────

  private BusinessException mapError(RestClientResponseException e) {
    try {
      String body = e.getResponseBodyAsString();
      int start = body.indexOf("\"code\":\"") + 8;
      int end = body.indexOf("\"", start);
      if (start > 7 && end > start) {
        String code = body.substring(start, end);
        return new BusinessException(ErrorCode.valueOf(code));
      }
    } catch (Exception ignored) {
    }
    return new BusinessException(ErrorCode.VALID_001);
  }
}
