package com.service.domain.user.controller;

import com.service.domain.user.dto.request.ConsentUpdateRequest;
import com.service.domain.user.dto.request.ProfileUpdateRequest;
import com.service.domain.user.dto.response.UserResponse;
import com.service.domain.user.service.UserService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getMe(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(userService.getMe(userId)));
  }

  @Operation(summary = "프로필 수정")
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
      Authentication authentication, @RequestBody ProfileUpdateRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
  }

  @Operation(summary = "알림 동의 수정")
  @PatchMapping("/me/consent")
  public ResponseEntity<ApiResponse<Void>> updateConsent(
      Authentication authentication, @Valid @RequestBody ConsentUpdateRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    userService.updateConsent(userId, request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "회원 탈퇴")
  @DeleteMapping("/me")
  public ResponseEntity<ApiResponse<Void>> withdraw(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    userService.withdraw(userId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
