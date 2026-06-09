package com.service.domain.admin.service;

import com.service.domain.admin.dto.response.ServiceHealthResponse;
import com.service.global.client.ServiceHealthClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceHealthService {

  private final ServiceHealthClient serviceHealthClient;
  private final Executor healthCheckExecutor;

  public AdminServiceHealthService(
      ServiceHealthClient serviceHealthClient,
      @Qualifier("healthCheckExecutor") Executor healthCheckExecutor) {
    this.serviceHealthClient = serviceHealthClient;
    this.healthCheckExecutor = healthCheckExecutor;
  }

  public List<ServiceHealthResponse> getAllServicesHealth() {
    CompletableFuture<ServiceHealthResponse> bankFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkBankServer, healthCheckExecutor);
    CompletableFuture<ServiceHealthResponse> stockFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkStockServer, healthCheckExecutor);
    CompletableFuture<ServiceHealthResponse> transactionFuture =
        CompletableFuture.supplyAsync(
            serviceHealthClient::checkTransactionServer, healthCheckExecutor);
    CompletableFuture<ServiceHealthResponse> mydataFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkMydataServer, healthCheckExecutor);
    CompletableFuture<ServiceHealthResponse> aiFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkAiServer, healthCheckExecutor);

    return List.of(
        bankFuture.join(),
        stockFuture.join(),
        transactionFuture.join(),
        mydataFuture.join(),
        aiFuture.join());
  }
}
