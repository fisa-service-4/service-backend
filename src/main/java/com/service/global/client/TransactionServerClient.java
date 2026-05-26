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
