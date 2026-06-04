package com.service.domain.transfer.controller;

import com.service.domain.transfer.dto.request.TransferRequest;
import com.service.domain.transfer.dto.response.TransferApproveResponse;
import com.service.domain.transfer.dto.response.TransferResponse;
import com.service.domain.transfer.dto.response.TransferResultResponse;
import com.service.domain.transfer.service.TransferService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfer", description = "이체 API")
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

  private final TransferService transferService;

  @Operation(summary = "이체 요청")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "이체 요청 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "TRANSFER_002: 잔액 부족 | ACCOUNT_003: 계좌 상태가 유효하지 않습니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "ACCOUNT_002: 본인 계좌가 아닙니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "ACCOUNT_001: 해당 계좌를 찾을 수 없습니다")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<TransferResponse>> requestTransfer(
      Authentication authentication,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody TransferRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(transferService.requestTransfer(userId, idempotencyKey, request)));
  }

  @Operation(summary = "이체 승인")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "이체 승인 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "TRANSFER_004: 본인 이체 건이 아닙니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "TRANSFER_001: 해당 이체 건을 찾을 수 없습니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "TRANSFER_003: 이미 처리 완료된 이체입니다")
  })
  @PostMapping("/{transferId}/approve")
  public ResponseEntity<ApiResponse<TransferApproveResponse>> approveTransfer(
      Authentication authentication, @PathVariable Long transferId) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(transferService.approveTransfer(userId, transferId)));
  }

  @Operation(summary = "이체 결과 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "이체 결과 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "TRANSFER_004: 본인 이체 건이 아닙니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "TRANSFER_001: 해당 이체 건을 찾을 수 없습니다")
  })
  @GetMapping("/{transferId}")
  public ResponseEntity<ApiResponse<TransferResultResponse>> getTransferResult(
      Authentication authentication, @PathVariable Long transferId) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(transferService.getTransferResult(userId, transferId)));
  }
}
