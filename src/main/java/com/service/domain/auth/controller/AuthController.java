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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "회원가입 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_001: 이미 가입된 이메일 | VALID_001: 입력값이 올바르지 않습니다")
  })
  @SecurityRequirements
  @PostMapping("/signup")
  public ResponseEntity<com.service.global.response.ApiResponse<SignupResponse>> signup(
      @Valid @RequestBody SignupRequest request) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(authService.signup(request)));
  }

  @Operation(summary = "휴대폰 인증 요청")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_002: 이미 가입된 전화번호 | AUTH_011: 본인 확인에 실패했습니다")
  })
  @SecurityRequirements
  @PostMapping("/phone/send")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> sendPhoneVerification(
      @Valid @RequestBody PhoneSendRequest request) {
    authService.sendPhoneVerification(request);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }

  @Operation(summary = "휴대폰 인증 검증")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "인증 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_006: 인증번호가 올바르지 않습니다 | AUTH_007: 인증번호가 만료되었습니다")
  })
  @SecurityRequirements
  @PostMapping("/phone/verify")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> verifyPhone(
      @Valid @RequestBody PhoneVerifyRequest request) {
    authService.verifyPhone(request);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }

  @Operation(summary = "로그인")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그인 성공"),
    @ApiResponse(responseCode = "400", description = "AUTH_003: 이메일 또는 비밀번호가 올바르지 않습니다")
  })
  @SecurityRequirements
  @PostMapping("/login")
  public ResponseEntity<com.service.global.response.ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(authService.login(request)));
  }

  @Operation(summary = "로그아웃")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "로그아웃 성공")})
  @PostMapping("/logout")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> logout(
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    authService.logout(userId);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }

  @Operation(summary = "토큰 재발급")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_004: 만료된 토큰입니다 | AUTH_005: 유효하지 않은 토큰입니다")
  })
  @SecurityRequirements
  @PostMapping("/reissue")
  public ResponseEntity<com.service.global.response.ApiResponse<TokenResponse>> reissue(
      @Valid @RequestBody ReissueRequest request) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            authService.reissue(request.getRefreshToken())));
  }

  @Operation(summary = "PIN 등록")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "PIN 등록 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_008: PIN이 일치하지 않습니다 | AUTH_010: 연속 또는 반복 숫자는 사용할 수 없습니다")
  })
  @PostMapping("/pin")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> registerPin(
      Authentication authentication, @Valid @RequestBody PinRegisterRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    authService.registerPin(userId, request);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }

  @Operation(summary = "회원가입 완료")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "회원가입 완료 성공")})
  @PostMapping("/signup/complete")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> completeSignup(
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    authService.completeSignup(userId);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }

  @Operation(summary = "PIN 검증")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "PIN 검증 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_008: PIN이 올바르지 않습니다 | AUTH_009: PIN이 잠겼습니다. 고객센터에 문의해주세요")
  })
  @PostMapping("/pin/verify")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> verifyPin(
      Authentication authentication, @Valid @RequestBody PinVerifyRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    authService.verifyPin(userId, request);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }

  @Operation(summary = "PIN 변경")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "PIN 변경 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_008: 기존 PIN이 올바르지 않습니다 | AUTH_010: 연속 또는 반복 숫자는 사용할 수 없습니다")
  })
  @PatchMapping("/pin")
  public ResponseEntity<com.service.global.response.ApiResponse<Void>> changePin(
      Authentication authentication, @Valid @RequestBody PinChangeRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    authService.changePin(userId, request);
    return ResponseEntity.ok(com.service.global.response.ApiResponse.success(null));
  }
}
