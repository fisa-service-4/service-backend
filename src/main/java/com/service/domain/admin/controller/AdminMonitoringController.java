package com.service.domain.admin.controller;

import com.service.domain.admin.dto.response.DashboardResponse;
import com.service.domain.admin.service.AdminLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Monitoring", description = "관리자 모니터링 API")
@RestController
@RequestMapping("/api/v1/admin/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {

  private final AdminLogService adminLogService;

  @Operation(summary = "관리자 대시보드 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/dashboard")
  public ResponseEntity<com.service.global.response.ApiResponse<DashboardResponse>> getDashboard() {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(adminLogService.getDashboard()));
  }
}
