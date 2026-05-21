package com.service.domain.auth.controller;

import com.service.domain.auth.dto.request.LoginRequest;
import com.service.domain.auth.dto.request.PhoneSendRequest;
import com.service.domain.auth.dto.request.PhoneVerifyRequest;
import com.service.domain.auth.dto.request.PinChangeRequest;
import com.service.domain.auth.dto.request.PinRegisterRequest;
import com.service.domain.auth.dto.request.PinVerifyRequest;
import com.service.domain.auth.dto.request.ReissueRequest;
import com.service.domain.auth.dto.request.SignupRequest;
import com.service.domain.auth.dto.response.LoginResponse;
import com.service.domain.auth.dto.response.SignupResponse;
import com.service.domain.auth.dto.response.TokenResponse;
import com.service.domain.auth.service.AuthService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "회원가입")
  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SignupResponse>> signup(
      @Valid @RequestBody SignupRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.signup(request)));
  }

  @Operation(summary = "휴대폰 인증 요청")
  @PostMapping("/phone/send")
  public ResponseEntity<ApiResponse<Void>> sendPhoneVerification(
      @Valid @RequestBody PhoneSendRequest request) {
    authService.sendPhoneVerification(request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "휴대폰 인증 검증")
  @PostMapping("/phone/verify")
  public ResponseEntity<ApiResponse<Void>> verifyPhone(
      @Valid @RequestBody PhoneVerifyRequest request) {
    authService.verifyPhone(request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "로그인")
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
  }

  @Operation(summary = "로그아웃")
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    authService.logout(userId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "토큰 재발급")
  @PostMapping("/reissue")
  public ResponseEntity<ApiResponse<TokenResponse>> reissue(
      @Valid @RequestBody ReissueRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.reissue(request.getRefreshToken())));
  }

  @Operation(summary = "PIN 등록")
  @PostMapping("/pin")
  public ResponseEntity<ApiResponse<Void>> registerPin(
      Authentication authentication, @Valid @RequestBody PinRegisterRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    authService.registerPin(userId, request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "회원가입 완료")
  @PostMapping("/signup/complete")
  public ResponseEntity<ApiResponse<Void>> completeSignup(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    authService.completeSignup(userId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "PIN 검증")
  @PostMapping("/pin/verify")
  public ResponseEntity<ApiResponse<Void>> verifyPin(
      Authentication authentication, @Valid @RequestBody PinVerifyRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    authService.verifyPin(userId, request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "PIN 변경")
  @PatchMapping("/pin")
  public ResponseEntity<ApiResponse<Void>> changePin(
      Authentication authentication, @Valid @RequestBody PinChangeRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    authService.changePin(userId, request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
