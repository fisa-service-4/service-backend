package com.service.domain.virtualsalary.controller;

import com.service.domain.virtualsalary.dto.request.ManualMatchingRequest;
import com.service.domain.virtualsalary.dto.response.ManualMatchingResponse;
import com.service.domain.virtualsalary.dto.response.PaymentMatchingResponse;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.service.PaymentMatchingService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PaymentMatching", description = "계약 입금 매칭 API")
@RestController
@RequestMapping("/api/v1/payment-matchings")
@RequiredArgsConstructor
public class PaymentMatchingController {

  private final PaymentMatchingService paymentMatchingService;

  @Operation(summary = "매칭 목록 조회", description = "계약 입금 매칭 목록을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "매칭 목록 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<List<PaymentMatchingResponse>>> getMatchings(
      Authentication authentication,
      @RequestParam(required = false) Long contractId,
      @RequestParam(required = false) MatchingStatus matchingStatus,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(
            paymentMatchingService.getMatchings(userId, contractId, matchingStatus, from, to)));
  }

  @Operation(summary = "완료 처리", description = "미입금 또는 금액 불일치 계약을 완료 처리합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "완료 처리 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "MATCHING_002: 이미 완료 처리된 건 | MATCHING_003: matchedBy는 USER만 허용"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "MATCHING_001: 매칭 정보를 찾을 수 없습니다")
  })
  @PatchMapping("/{matchingId}/manual")
  public ResponseEntity<ApiResponse<ManualMatchingResponse>> manualMatch(
      Authentication authentication,
      @PathVariable Long matchingId,
      @Valid @RequestBody ManualMatchingRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(paymentMatchingService.manualMatch(userId, matchingId, request)));
  }
}
