package com.service.domain.stock.dto.response;

import com.service.domain.stock.entity.FavoriteStock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStockAddResponse {

  private Long favoriteId;
  private String stockCode;

  public static FavoriteStockAddResponse from(FavoriteStock entity) {
    return FavoriteStockAddResponse.builder()
        .favoriteId(entity.getFavoriteStockId())
        .stockCode(entity.getStockCode())
        .build();
  }
}
