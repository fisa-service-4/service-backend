package com.service.domain.admin.controller;

import com.service.domain.admin.dto.request.ErrorLogResolveRequest;
import com.service.domain.admin.dto.response.AdminAiChatSessionResponse;
import com.service.domain.admin.dto.response.AdminStockOrderLogResponse;
import com.service.domain.admin.dto.response.AdminTransferLogResponse;
import com.service.domain.admin.dto.response.ApiLogResponse;
import com.service.domain.admin.dto.response.ErrorLogResponse;
import com.service.domain.admin.dto.response.LoginLogResponse;
import com.service.domain.admin.service.AdminLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Log", description = "관리자 로그 관리 API")
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

  private final AdminLogService adminLogService;

  @Operation(summary = "로그인 로그 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/login")
  public ResponseEntity<com.service.global.response.ApiResponse<Page<LoginLogResponse>>>
      getLoginLogs(
          @RequestParam(required = false) Long userId,
          @RequestParam(required = false) String loginType,
          @RequestParam(required = false) String startDate,
          @RequestParam(required = false) String endDate,
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminLogService.getLoginLogs(userId, loginType, startDate, endDate, pageable)));
  }

  @Operation(summary = "AI 채팅 세션 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/ai")
  public ResponseEntity<com.service.global.response.ApiResponse<Page<AdminAiChatSessionResponse>>>
      getAiChatSessions(
          @RequestParam(required = false) String sessionType,
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminLogService.getAiChatSessions(sessionType, pageable)));
  }

  @Operation(summary = "오류 로그 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/error")
  public ResponseEntity<com.service.global.response.ApiResponse<Page<ErrorLogResponse>>>
      getErrorLogs(
          @RequestParam(required = false) String errorLevel,
          @RequestParam(required = false) Boolean resolvedYn,
          @RequestParam(required = false) String startDate,
          @RequestParam(required = false) String endDate,
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminLogService.getErrorLogs(errorLevel, resolvedYn, startDate, endDate, pageable)));
  }

  @Operation(summary = "오류 해결 처리")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "해결 처리 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요"),
    @ApiResponse(responseCode = "404", description = "오류 로그를 찾을 수 없음")
  })
  @PatchMapping("/error/{id}")
  public ResponseEntity<com.service.global.response.ApiResponse<ErrorLogResponse>> resolveErrorLog(
      @PathVariable Long id, @Valid @RequestBody ErrorLogResolveRequest request) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminLogService.resolveErrorLog(id, request)));
  }

  @Operation(summary = "API 로그 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/api")
  public ResponseEntity<com.service.global.response.ApiResponse<Page<ApiLogResponse>>> getApiLogs(
      @RequestParam(required = false) String serviceName,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminLogService.getApiLogs(serviceName, startDate, endDate, pageable)));
  }

  @Operation(summary = "거래 이력 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/transfers")
  public ResponseEntity<com.service.global.response.ApiResponse<Page<AdminTransferLogResponse>>>
      getTransferHistory(
          @RequestParam(required = false) String startDate,
          @RequestParam(required = false) String endDate,
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminLogService.getTransferHistory(startDate, endDate, pageable)));
  }

  @Operation(summary = "주식 거래 이력 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/orders")
  public ResponseEntity<com.service.global.response.ApiResponse<Page<AdminStockOrderLogResponse>>>
      getStockOrderHistory(
          @RequestParam(required = false) String startDate,
          @RequestParam(required = false) String endDate,
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminLogService.getStockOrderHistory(startDate, endDate, pageable)));
  }
}
