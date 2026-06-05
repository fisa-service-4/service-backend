package com.service.domain.stock.controller;

import com.service.domain.stock.dto.response.CashBalanceResponse;
import com.service.domain.stock.dto.response.StockAccountsResponse;
import com.service.domain.stock.service.StockService;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserRepository;
import com.service.global.exception.ErrorCode;
import com.service.global.exception.BusinessException;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stock", description = "증권 API")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

  private final StockService stockService;
  private final UserRepository userRepository;

  @Operation(summary = "예수금 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/cash-balance")
  public ResponseEntity<ApiResponse<CashBalanceResponse>> getCashBalance(
      @RequestParam Long accountId) {
    return ResponseEntity.ok(ApiResponse.success(stockService.getCashBalance(accountId)));
  }

  @Operation(summary = "주문 가능 계좌 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/accounts")
  public ResponseEntity<ApiResponse<StockAccountsResponse>> getStockAccounts(
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    return ResponseEntity.ok(ApiResponse.success(stockService.getStockAccounts(user.getFirebaseUid())));
  }
}
