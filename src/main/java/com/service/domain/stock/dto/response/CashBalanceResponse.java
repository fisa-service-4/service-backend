package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.CashBalanceItem;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashBalanceResponse {

  private BigDecimal cashBalance;
  private BigDecimal availableBalance;

  public static CashBalanceResponse from(CashBalanceItem item) {
    return CashBalanceResponse.builder()
        .cashBalance(item.getCashBalance())
        .availableBalance(item.getAvailableBalance())
        .build();
  }
}
