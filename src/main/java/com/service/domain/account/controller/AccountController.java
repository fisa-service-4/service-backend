package com.service.domain.account.controller;

import com.service.domain.account.dto.request.AccountRoleUpdateRequest;
import com.service.domain.account.dto.response.AccountListResponse;
import com.service.domain.account.dto.response.AccountRoleUpdateResponse;
import com.service.domain.account.dto.response.AccountTransactionResponse;
import com.service.domain.account.service.AccountService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account", description = "계좌 API")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @Operation(summary = "내 계좌 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<List<AccountListResponse>>> getMyAccounts(
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(accountService.getMyAccounts(userId)));
  }

  @Operation(summary = "계좌 거래내역 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "ACCOUNT_002: 본인 계좌가 아닙니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "ACCOUNT_001: 해당 계좌를 찾을 수 없습니다")
  })
  @GetMapping("/{accountId}/transactions")
  public ResponseEntity<ApiResponse<List<AccountTransactionResponse>>> getTransactions(
      Authentication authentication,
      @PathVariable Long accountId,
      @RequestParam @Nullable String fromDate,
      @RequestParam @Nullable String toDate) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(accountService.getTransactions(userId, accountId, fromDate, toDate)));
  }

  @Operation(summary = "계좌 역할 설정")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "역할 설정 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "ACCOUNT_002: 본인 계좌가 아닙니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "ACCOUNT_001: 해당 계좌를 찾을 수 없습니다")
  })
  @PatchMapping("/{accountId}/role")
  public ResponseEntity<ApiResponse<AccountRoleUpdateResponse>> updateAccountRole(
      Authentication authentication,
      @PathVariable Long accountId,
      @Valid @RequestBody AccountRoleUpdateRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(accountService.updateAccountRole(userId, accountId, request)));
  }
}
