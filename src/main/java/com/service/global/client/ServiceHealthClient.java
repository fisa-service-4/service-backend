package com.service.global.client;

import com.service.domain.admin.dto.response.ServiceHealthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ServiceHealthClient {

  private final RestClient healthCheckRestClient;

  @Value("${transaction-server.url}")
  private String transactionServerUrl;

  @Value("${mydata.server.url}")
  private String mydataServerUrl;

  @Value("${ai.server.url}")
  private String aiServerUrl;

  public ServiceHealthClient(@Qualifier("healthCheckRestClient") RestClient healthCheckRestClient) {
    this.healthCheckRestClient = healthCheckRestClient;
  }

  public ServiceHealthResponse checkBankServer() {
    return check("bank-server", transactionServerUrl + "/baas/v1/health/bank");
  }

  public ServiceHealthResponse checkStockServer() {
    return check("stock-server", transactionServerUrl + "/baas/v1/health/stock");
  }

  public ServiceHealthResponse checkTransactionServer() {
    return check("transaction-server", transactionServerUrl + "/baas/v1/health");
  }

  public ServiceHealthResponse checkMydataServer() {
    return check("mydata-server", mydataServerUrl + "/health");
  }

  public ServiceHealthResponse checkAiServer() {
    return check("ai-server", aiServerUrl + "/health");
  }

  private ServiceHealthResponse check(String serviceName, String url) {
    try {
      healthCheckRestClient.get().uri(url).retrieve().toBodilessEntity();
      return new ServiceHealthResponse(serviceName, "UP", null);
    } catch (Exception e) {
      return new ServiceHealthResponse(serviceName, "DOWN", e.getMessage());
    }
  }
}
