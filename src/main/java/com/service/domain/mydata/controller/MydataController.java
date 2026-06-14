package com.service.domain.mydata.controller;

import com.service.domain.mydata.dto.response.MydataConnectionResponse;
import com.service.domain.mydata.service.MydataService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyData", description = "마이데이터 API")
@RestController
@RequestMapping("/api/v1/mydata")
@RequiredArgsConstructor
public class MydataController {

  private final MydataService mydataService;

  @Operation(
      summary = "마이데이터 전체 연동",
      description = "사용자의 Firebase UID 기반으로 mydata-server에 금융기관 연동을 요청합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "연동 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "USER_001: 사용자를 찾을 수 없습니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "SERVER_001: 외부 서버와 통신 중 오류가 발생했습니다")
  })
  @PostMapping("/connect")
  public ResponseEntity<ApiResponse<Void>> connectAll(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    mydataService.connectAll(userId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(
      summary = "연동 계좌 목록 조회",
      description = "mydata-server에서 연동된 은행/증권 계좌 목록을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "USER_001: 사용자를 찾을 수 없습니다"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "SERVER_001: 외부 서버와 통신 중 오류가 발생했습니다")
  })
  @GetMapping("/connections")
  public ResponseEntity<ApiResponse<MydataConnectionResponse>> getConnections(
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(mydataService.getConnections(userId)));
  }
}
