package com.service.domain.mydata.dto.response;

import com.service.global.client.BankServerClient;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MydataConnectionResponse {
  private List<BankServerClient.BankAccountItem> bankAccounts;
  private List<BankServerClient.StockAccountItem> stockAccounts;
}
