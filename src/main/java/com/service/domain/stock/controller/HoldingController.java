package com.service.domain.stock.controller;

import com.service.domain.stock.dto.response.HoldingListResponse;
import com.service.domain.stock.dto.response.HoldingReturnsResponse;
import com.service.domain.stock.service.HoldingService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Holding", description = "보유 종목 API")
@RestController
@RequestMapping("/api/v1/holdings")
@RequiredArgsConstructor
public class HoldingController {

  private final HoldingService holdingService;

  @Operation(summary = "보유 종목 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<HoldingListResponse>> getHoldings(
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
    return ResponseEntity.ok(ApiResponse.success(holdingService.getHoldings(authorization)));
  }

  @Operation(summary = "수익률 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/returns")
  public ResponseEntity<ApiResponse<HoldingReturnsResponse>> getReturns(
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
    return ResponseEntity.ok(ApiResponse.success(holdingService.getReturns(authorization)));
  }
}
