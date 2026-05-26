package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.CandleItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockChartResponse {

  private String stockCode;
  private List<CandleItem> candles;

  public static StockChartResponse of(String stockCode, List<CandleItem> candles) {
    return StockChartResponse.builder().stockCode(stockCode).candles(candles).build();
  }
}
