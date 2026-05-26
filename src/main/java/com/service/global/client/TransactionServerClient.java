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
  // 종목 검색
  // ───────────────────────────────────────────────

  public List<StockItem> searchStocks(String authorization, String keyword) {
    try {
      TxResponse<TxContentData<StockItem>> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stocks/search?keyword={keyword}", keyword)
              .header("Authorization", authorization)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData().getContent();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

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

  // ───────────────────────────────────────────────
  // 주문 취소
  // ───────────────────────────────────────────────

  public OrderCancelItem cancelOrder(
      String authorization, String pinToken, String idempotencyKey, Long orderId) {
    try {
      TxResponse<OrderCancelItem> response =
          transactionServerRestClient
              .post()
              .uri("/baas/v1/stocks/orders/{orderId}/cancel", orderId)
              .header("Authorization", authorization)
              .header("Pin-Token", pinToken)
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
      String authorization, String pinToken, String idempotencyKey,
      com.service.domain.stock.dto.request.OrderCreateRequest request) {
    try {
      TxResponse<OrderItem> response =
          transactionServerRestClient
              .post()
              .uri("/baas/v1/stocks/orders")
              .header("Authorization", authorization)
              .header("Pin-Token", pinToken)
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

  public CashBalanceItem getCashBalance(String authorization) {
    try {
      TxResponse<CashBalanceItem> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stocks/cash-balance")
              .header("Authorization", authorization)
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

  public List<StockAccountItem> getStockAccounts(String authorization) {
    try {
      TxResponse<TxContentData<StockAccountItem>> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stocks/accounts")
              .header("Authorization", authorization)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData().getContent();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 차트 조회
  // ───────────────────────────────────────────────

  public List<CandleItem> getStockChart(
      String authorization, String stockCode, String interval, String from, String to) {
    try {
      TxResponse<TxContentData<CandleItem>> response =
          transactionServerRestClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/baas/v1/stocks/{stockCode}/charts")
                          .queryParam("interval", interval)
                          .queryParamIfPresent("fromDate", java.util.Optional.ofNullable(from))
                          .queryParamIfPresent("toDate", java.util.Optional.ofNullable(to))
                          .build(stockCode))
              .header("Authorization", authorization)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return response.getData().getContent();
    } catch (RestClientResponseException e) {
      throw mapError(e);
    }
  }

  // ───────────────────────────────────────────────
  // 현재가 조회
  // ───────────────────────────────────────────────

  public StockPriceItem getStockPrice(String authorization, String stockCode) {
    try {
      TxResponse<StockPriceItem> response =
          transactionServerRestClient
              .get()
              .uri("/baas/v1/stocks/{stockCode}/price", stockCode)
              .header("Authorization", authorization)
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
  public static class StockItem {
    private String stockCode;
    private String stockName;
    private String market;
    private java.math.BigDecimal currentPrice;
    private java.math.BigDecimal changeRate;
  }

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
  public static class CandleItem {
    private String date;
    private java.math.BigDecimal open;
    private java.math.BigDecimal high;
    private java.math.BigDecimal low;
    private java.math.BigDecimal close;
    private Long volume;
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
