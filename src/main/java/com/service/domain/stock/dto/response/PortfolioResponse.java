package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.PortfolioItem;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

  private BigDecimal totalAsset;
  private BigDecimal cashAsset;
  private BigDecimal stockAsset;
  private BigDecimal savingAsset;
  private BigDecimal availableCash;
  private AssetRatio assetRatio;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AssetRatio {
    private Integer cash;
    private Integer stock;
    private Integer saving;
  }

  public static PortfolioResponse from(PortfolioItem item) {
    return PortfolioResponse.builder()
        .totalAsset(item.getTotalAsset())
        .cashAsset(item.getCashAsset())
        .stockAsset(item.getStockAsset())
        .savingAsset(item.getSavingAsset())
        .availableCash(item.getAvailableCash())
        .assetRatio(
            AssetRatio.builder()
                .cash(item.getAssetRatio().getCash())
                .stock(item.getAssetRatio().getStock())
                .saving(item.getAssetRatio().getSaving())
                .build())
        .build();
  }
}
