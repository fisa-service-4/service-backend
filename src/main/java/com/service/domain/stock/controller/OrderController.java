package com.service.domain.stock.controller;

import com.service.domain.stock.dto.request.OrderCreateRequest;
import com.service.domain.stock.dto.response.OrderCancelResponse;
import com.service.domain.stock.dto.response.OrderDetailResponse;
import com.service.domain.stock.dto.response.OrderListResponse;
import com.service.domain.stock.dto.response.OrderResponse;
import com.service.domain.stock.service.OrderService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestParam Long accountId,
      @Valid @RequestBody OrderCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(orderService.createOrder(idempotencyKey, accountId, request)));
  }

  @Operation(summary = "주문 상세 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/{orderId}")
  public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
      @PathVariable Long orderId) {
    return ResponseEntity.ok(ApiResponse.success(orderService.getOrderDetail(orderId)));
  }

  @Operation(summary = "주문 내역 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
      @RequestParam Long accountId,
      @Nullable @RequestParam(required = false) String status,
      @Nullable @RequestParam(required = false) String orderType,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(orderService.getOrders(accountId, status, orderType, page, size)));
  }

  @Operation(summary = "주문 취소")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "취소 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @PostMapping("/{orderId}/cancel")
  public ResponseEntity<ApiResponse<OrderCancelResponse>> cancelOrder(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @PathVariable Long orderId) {
    return ResponseEntity.ok(
        ApiResponse.success(orderService.cancelOrder(idempotencyKey, orderId)));
  }
}
