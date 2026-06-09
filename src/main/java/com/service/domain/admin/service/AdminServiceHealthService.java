package com.service.domain.admin.service;

import com.service.domain.admin.dto.response.ServiceHealthResponse;
import com.service.global.client.ServiceHealthClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceHealthService {

  private final ServiceHealthClient serviceHealthClient;

  public List<ServiceHealthResponse> getAllServicesHealth() {
    CompletableFuture<ServiceHealthResponse> bankFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkBankServer);
    CompletableFuture<ServiceHealthResponse> stockFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkStockServer);
    CompletableFuture<ServiceHealthResponse> transactionFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkTransactionServer);
    CompletableFuture<ServiceHealthResponse> mydataFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkMydataServer);
    CompletableFuture<ServiceHealthResponse> aiFuture =
        CompletableFuture.supplyAsync(serviceHealthClient::checkAiServer);

    return List.of(
        bankFuture.join(),
        stockFuture.join(),
        transactionFuture.join(),
        mydataFuture.join(),
        aiFuture.join());
  }
}
