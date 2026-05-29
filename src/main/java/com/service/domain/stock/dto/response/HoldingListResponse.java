package com.service.domain.stock.dto.response;

import com.service.global.client.TransactionServerClient.HoldingItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingListResponse {

  private List<HoldingItem> holdings;

  public static HoldingListResponse from(List<HoldingItem> holdings) {
    return HoldingListResponse.builder().holdings(holdings).build();
  }
}
