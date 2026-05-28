package com.service.domain.stock.controller;

import com.service.domain.stock.dto.response.ExecutionListResponse;
import com.service.domain.stock.service.ExecutionService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Execution", description = "체결 API")
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
public class ExecutionController {

  private final ExecutionService executionService;

  @Operation(summary = "체결 내역 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<ExecutionListResponse>> getExecutions(
      @RequestParam Long accountId,
      @Nullable @RequestParam(required = false) String stockCode,
      @Nullable @RequestParam(required = false) String from,
      @Nullable @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            executionService.getExecutions(accountId, stockCode, from, to, page, size)));
  }
}
