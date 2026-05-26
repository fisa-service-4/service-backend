package com.service.domain.stock.controller;

import com.service.domain.stock.dto.request.OrderCreateRequest;
import com.service.domain.stock.dto.response.OrderResponse;
import com.service.domain.stock.service.OrderService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @Operation(summary = "주식 주문 생성")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "주문 생성 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "ORDER_001: 주문 가능 금액 부족 | ORDER_002: 보유 수량 부족"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization,
      @RequestHeader("Pin-Token") String pinToken,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody OrderCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(orderService.createOrder(authorization, pinToken, idempotencyKey, request)));
  }
}
