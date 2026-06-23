package com.service.domain.mydata.controller;

import com.service.domain.mydata.service.TransactionSyncService;
import com.service.global.response.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevSyncController {

  private final TransactionSyncService transactionSyncService;

  @PostMapping("/sync/transactions")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> syncTransactions() {
    transactionSyncService.syncAll();
    return ResponseEntity.ok(ApiResponse.success(Map.of("synced", true)));
  }
}
