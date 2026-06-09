package com.service.domain.admin.controller;

import com.service.domain.admin.dto.response.ServiceHealthResponse;
import com.service.domain.admin.service.AdminServiceHealthService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Service Health", description = "관리자 서비스 헬스체크 API")
@RestController
@RequestMapping("/api/v1/admin/services")
@RequiredArgsConstructor
public class AdminServiceHealthController {

  private final AdminServiceHealthService adminServiceHealthService;

  @Operation(summary = "전체 서비스 헬스체크")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping("/health")
  public ResponseEntity<ApiResponse<List<ServiceHealthResponse>>> getServicesHealth() {
    return ResponseEntity.ok(
        ApiResponse.success(adminServiceHealthService.getAllServicesHealth()));
  }
}
