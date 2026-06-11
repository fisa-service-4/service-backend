package com.service.global.client;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

  public ConnectionsData getConnections(String firebaseUid) {
    String url = mydataServerUrl + "/connections";
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Firebase-Uid", firebaseUid);
    ResponseEntity<ConnectionsWrapper> response =
        restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});

    ConnectionsWrapper body = response.getBody();
    if (body == null || !body.isSuccess() || body.getData() == null) {
      throw new RuntimeException("mydata-server 계좌 목록 조회 실패: firebaseUid=" + firebaseUid);
    }
    return body.getData();
  }

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

  public BankAccountDetailData getBankAccountDetail(Long accountId) {
    String url = mydataServerUrl + "/bank/accounts/" + accountId;
    ResponseEntity<BankAccountDetailWrapper> response =
        restTemplate.exchange(
            url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {});

    BankAccountDetailWrapper body = response.getBody();
    if (body == null || !body.isSuccess() || body.getData() == null) {
      throw new RuntimeException("mydata-server 계좌 상세 조회 실패: accountId=" + accountId);
    }
    return body.getData();
  }

  public StockAccountItem getStockAccountDetail(String firebaseUid, Long accountId) {
    ConnectionsData connections = getConnections(firebaseUid);
    if (connections.getStockAccounts() == null) {
      throw new RuntimeException("투자 계좌 조회 실패: 계좌 목록 없음");
    }
    return connections.getStockAccounts().stream()
        .filter(a -> accountId.equals(a.getAccountId()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("투자 계좌 조회 실패: accountId=" + accountId));
  }

  @Getter
  @NoArgsConstructor
  static class ConnectionsWrapper {
    private boolean success;
    private ConnectionsData data;
  }

  @Getter
  @NoArgsConstructor
  public static class ConnectionsData {
    private List<BankAccountItem> bankAccounts;
    private List<StockAccountItem> stockAccounts;
  }

  @Getter
  @NoArgsConstructor
  public static class BankAccountItem {
    private Long accountId;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private BigDecimal balance;
  }

  @Getter
  @NoArgsConstructor
  public static class StockAccountItem {
    private Long accountId;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private BigDecimal cashBalance;
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

  @Getter
  @NoArgsConstructor
  static class BankAccountDetailWrapper {
    private boolean success;
    private BankAccountDetailData data;
  }

  @Getter
  @NoArgsConstructor
  public static class BankAccountDetailData {
    private Long accountId;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String accountStatus;
    private BigDecimal balance;
  }
}
