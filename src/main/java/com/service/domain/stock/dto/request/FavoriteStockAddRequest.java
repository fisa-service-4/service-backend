package com.service.domain.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FavoriteStockAddRequest {

  @NotBlank
  private String stockCode;
}
