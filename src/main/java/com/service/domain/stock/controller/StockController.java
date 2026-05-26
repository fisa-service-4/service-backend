package com.service.domain.stock.controller;

import com.service.domain.stock.dto.response.CashBalanceResponse;
import com.service.domain.stock.dto.response.StockAccountsResponse;
import com.service.domain.stock.dto.response.StockChartResponse;
import com.service.domain.stock.dto.response.StockPriceResponse;
import com.service.domain.stock.dto.response.StockSearchResponse;
import com.service.domain.stock.service.StockService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stock", description = "증권 API")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

  private final StockService stockService;

  @Operation(summary = "종목 검색")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "검색 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<StockSearchResponse>> searchStocks(
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization,
      @RequestParam String keyword) {
    return ResponseEntity.ok(
        ApiResponse.success(stockService.searchStocks(authorization, keyword)));
  }

  @Operation(summary = "현재가 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/{stockCode}/price")
  public ResponseEntity<ApiResponse<StockPriceResponse>> getStockPrice(
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization,
      @PathVariable String stockCode) {
    return ResponseEntity.ok(
        ApiResponse.success(stockService.getStockPrice(authorization, stockCode)));
  }

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
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
    return ResponseEntity.ok(
        ApiResponse.success(stockService.getCashBalance(authorization)));
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
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
    return ResponseEntity.ok(
        ApiResponse.success(stockService.getStockAccounts(authorization)));
  }

  @Operation(summary = "차트 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/{stockCode}/chart")
  public ResponseEntity<ApiResponse<StockChartResponse>> getStockChart(
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization,
      @PathVariable String stockCode,
      @RequestParam String interval,
      @Nullable @RequestParam(required = false) String from,
      @Nullable @RequestParam(required = false) String to) {
    return ResponseEntity.ok(
        ApiResponse.success(stockService.getStockChart(authorization, stockCode, interval, from, to)));
  }
}
