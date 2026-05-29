package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.ReturnsItem;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingReturnsResponse {

  private BigDecimal dailyReturnRate;
  private BigDecimal monthlyReturnRate;
  private BigDecimal yearlyReturnRate;

  public static HoldingReturnsResponse from(ReturnsItem item) {
    return HoldingReturnsResponse.builder()
        .dailyReturnRate(item.getDailyReturnRate())
        .monthlyReturnRate(item.getMonthlyReturnRate())
        .yearlyReturnRate(item.getYearlyReturnRate())
        .build();
  }
}
