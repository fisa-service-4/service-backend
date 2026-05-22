package com.service.domain.user.controller;

import com.service.domain.user.dto.request.ConsentUpdateRequest;
import com.service.domain.user.dto.response.UserResponse;
import com.service.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "내 정보 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "401", description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @ApiResponse(responseCode = "404", description = "USER_001: 사용자를 찾을 수 없음")
  })
  @GetMapping("/me")
  public ResponseEntity<com.service.global.response.ApiResponse<UserResponse>> getMe(
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(userService.getMe(userId)));
  }

  @Operation(summary = "알람 on/off")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "알림 동의 수정 성공"),
    @ApiResponse(responseCode = "401", description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @ApiResponse(responseCode = "404", description = "USER_001: 사용자를 찾을 수 없음")
  })
  @PatchMapping("/me/consent")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> updateConsent(
      Authentication authentication, @Valid @RequestBody ConsentUpdateRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    userService.updateConsent(userId, request);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }
}
