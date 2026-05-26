package com.service.domain.stock.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

  @NotBlank
  private String stockCode;

  @NotBlank
  private String orderType;

  @NotBlank
  private String orderMethod;

  @NotNull
  @Min(1)
  private Integer quantity;

  private BigDecimal price;
}
