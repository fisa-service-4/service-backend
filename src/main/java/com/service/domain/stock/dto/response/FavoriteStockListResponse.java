package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.StockPriceItem;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStockListResponse {

  private List<FavoriteStockItem> favorites;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FavoriteStockItem {
    private Long favoriteId;
    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;

    public static FavoriteStockItem of(Long favoriteId, StockPriceItem price) {
      return FavoriteStockItem.builder()
          .favoriteId(favoriteId)
          .stockCode(price.getStockCode())
          .stockName(price.getStockName())
          .currentPrice(price.getCurrentPrice())
          .changeRate(price.getChangeRate())
          .build();
    }
  }
}
