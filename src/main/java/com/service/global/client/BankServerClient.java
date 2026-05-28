package com.service.global.client;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class BankServerClient {

  private final RestTemplate restTemplate;

  @Value("${mydata.server.url}")
  private String mydataServerUrl;

  public BigDecimal getAccountBalance(Long accountId) {
    String url = mydataServerUrl + "/bank/accounts/" + accountId + "/balance";
    ResponseEntity<BankBalanceWrapper> response =
        restTemplate.exchange(
            url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {});

    BankBalanceWrapper body = response.getBody();
    if (body == null || !body.isSuccess() || body.getData() == null) {
      throw new RuntimeException("mydata-server 잔액 조회 실패: accountId=" + accountId);
    }
    return body.getData().getBalance();
  }

  @Getter
  @NoArgsConstructor
  static class BankBalanceWrapper {
    private boolean success;
    private BankBalanceData data;
  }

  @Getter
  @NoArgsConstructor
  static class BankBalanceData {
    private Long accountId;
    private BigDecimal balance;
  }
}
