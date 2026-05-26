package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.StockPriceItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponse {

  private String stockCode;
  private String stockName;
  private BigDecimal currentPrice;
  private BigDecimal changeRate;
  private LocalDateTime updatedAt;

  public static StockPriceResponse from(StockPriceItem item) {
    return StockPriceResponse.builder()
        .stockCode(item.getStockCode())
        .stockName(item.getStockName())
        .currentPrice(item.getCurrentPrice())
        .changeRate(item.getChangeRate())
        .updatedAt(item.getUpdatedAt())
        .build();
  }
}
