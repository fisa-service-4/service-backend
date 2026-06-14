package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.auth.dto.request.LoginRequest;
import com.service.domain.auth.dto.response.LoginResponse;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.security.JwtProvider;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private JwtProvider jwtProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private TransactionServerClient transactionServerClient;
  @Mock private AdminLogSaveService adminLogSaveService;
  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private AuthService authService;

  // ────────────────────────────────────────────────────────────────────────────
  // B-01. 정상 로그인
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("B-01: 정상 로그인 - 토큰 발급, Redis 저장, resolveLoginFailureLogs 호출 확인")
  void login_success() {
    System.out.println("\n=== B-01: 정상 로그인 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] LoginRequest 생성 (email=user@test.com, password=Password123!)");
    LoginRequest request = new LoginRequest();
    ReflectionTestUtils.setField(request, "email", "user@test.com");
    ReflectionTestUtils.setField(request, "password", "Password123!");

    User user =
        User.builder()
            .userId(1L)
            .firebaseUid("firebase-uid")
            .email("user@test.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .phoneNumber("01012345678")
            .role(User.Role.USER)
            .status(User.Status.ACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();

    System.out.println("[Given] userRepository.findByEmail() = User 존재");
    given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));

    System.out.println("[Given] passwordEncoder.matches() = true");
    given(passwordEncoder.matches("Password123!", "hashed-password")).willReturn(true);

    System.out.println("[Given] jwtProvider 토큰 반환값 설정");
    given(jwtProvider.generateAccessToken(1L, "USER")).willReturn("access-token");
    given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token");

    System.out.println("[Given] redisTemplate.opsForValue() stub 설정");
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.login(request) 호출");
    LoginResponse response = authService.login(request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then 1] Redis 저장 확인 (key=refresh:1, value=refresh-token, TTL=7일)");
    then(valueOperations).should().set("refresh:1", "refresh-token", 7L, TimeUnit.DAYS);
    System.out.println("        ✓ redisTemplate.opsForValue().set(\"refresh:1\", \"refresh-token\", 7, DAYS) 호출됨");

    System.out.println("[Then 2] resolveLoginFailureLogs(1L) 호출 확인");
    then(adminLogSaveService).should().resolveLoginFailureLogs(1L);
    System.out.println("        ✓ resolveLoginFailureLogs(userId=1) 호출됨");

    System.out.println("[Then 3] LoginResponse 응답값 검증");
    assertThat(response.getAccessToken()).isEqualTo("access-token");
    assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    assertThat(response.getUserId()).isEqualTo(1L);
    assertThat(response.getUserName()).isEqualTo("홍길동");
    assertThat(response.getRole()).isEqualTo("USER");
    System.out.println("        ✓ accessToken = access-token");
    System.out.println("        ✓ refreshToken = refresh-token");
    System.out.println("        ✓ userId = 1");
    System.out.println("        ✓ userName = 홍길동");
    System.out.println("        ✓ role = USER");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // B-02. 존재하지 않는 이메일
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("B-02: 존재하지 않는 이메일 - AUTH_003 예외 발생 및 saveSystemErrorLog 호출 확인")
  void login_emailNotFound() {
    System.out.println("\n=== B-02: 존재하지 않는 이메일 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] LoginRequest 생성 (존재하지 않는 이메일)");
    LoginRequest request = new LoginRequest();
    ReflectionTestUtils.setField(request, "email", "unknown@test.com");
    ReflectionTestUtils.setField(request, "password", "Password123!");

    System.out.println("[Given] userRepository.findByEmail() = Optional.empty()");
    given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.login(request) 호출");
    System.out.println("[Then] BusinessException(AUTH_003) 발생 확인");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(com.service.global.exception.BusinessException.class)
        .extracting(ex -> ((com.service.global.exception.BusinessException) ex).getErrorCode())
        .isEqualTo(com.service.global.exception.ErrorCode.AUTH_003);
    System.out.println("       ✓ BusinessException 발생");
    System.out.println("       ✓ ErrorCode = AUTH_003 (이메일 또는 비밀번호가 올바르지 않습니다)");

    System.out.println("[Then] saveSystemErrorLog(null, WARN, AUTH_003, ..., userId=null) 호출 확인");
    then(adminLogSaveService)
        .should()
        .saveSystemErrorLog(
            null,
            "WARN",
            "AUTH_003",
            "이메일 또는 비밀번호가 올바르지 않습니다.",
            "/api/v1/auth/login",
            null);
    System.out.println("       ✓ saveSystemErrorLog 호출됨 (userId=null — 사용자 특정 불가)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // B-03. 비밀번호 불일치
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("B-03: 비밀번호 불일치 - AUTH_003 예외 발생, userId 포함 에러 로그 저장, Redis 미호출")
  void login_passwordMismatch() {
    System.out.println("\n=== B-03: 비밀번호 불일치 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] LoginRequest 생성 (잘못된 비밀번호)");
    LoginRequest request = new LoginRequest();
    ReflectionTestUtils.setField(request, "email", "user@test.com");
    ReflectionTestUtils.setField(request, "password", "WrongPassword!");

    User user =
        User.builder()
            .userId(1L)
            .firebaseUid("firebase-uid")
            .email("user@test.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .phoneNumber("01012345678")
            .role(User.Role.USER)
            .status(User.Status.ACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();

    System.out.println("[Given] userRepository.findByEmail() = User 존재 (userId=1)");
    given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));

    System.out.println("[Given] passwordEncoder.matches(\"WrongPassword!\", hash) = false");
    given(passwordEncoder.matches("WrongPassword!", "hashed-password")).willReturn(false);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.login(request) 호출");
    System.out.println("[Then] BusinessException(AUTH_003) 발생 확인");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(com.service.global.exception.BusinessException.class)
        .extracting(ex -> ((com.service.global.exception.BusinessException) ex).getErrorCode())
        .isEqualTo(com.service.global.exception.ErrorCode.AUTH_003);
    System.out.println("       ✓ BusinessException 발생");
    System.out.println("       ✓ ErrorCode = AUTH_003");

    System.out.println("[Then] saveSystemErrorLog(null, WARN, AUTH_003, ..., userId=1) 호출 확인");
    then(adminLogSaveService)
        .should()
        .saveSystemErrorLog(
            null,
            "WARN",
            "AUTH_003",
            "이메일 또는 비밀번호가 올바르지 않습니다.",
            "/api/v1/auth/login",
            1L);
    System.out.println("       ✓ saveSystemErrorLog 호출됨 (userId=1 — 사용자 특정됨)");

    System.out.println("[Then] Redis 저장 미호출 확인");
    then(redisTemplate).shouldHaveNoInteractions();
    System.out.println("       ✓ redisTemplate 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // B-04. 로그인 성공 시 실패 이력 해소
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("B-04: 로그인 성공 시 실패 이력 해소 - resolveLoginFailureLogs 호출, saveSystemErrorLog 미호출")
  void login_resolveFailureLogsOnSuccess() {
    System.out.println("\n=== B-04: 로그인 성공 시 실패 이력 해소 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 정상 LoginRequest 생성 (이전에 로그인 실패 이력 있는 사용자)");
    LoginRequest request = new LoginRequest();
    ReflectionTestUtils.setField(request, "email", "user@test.com");
    ReflectionTestUtils.setField(request, "password", "Password123!");

    User user =
        User.builder()
            .userId(1L)
            .firebaseUid("firebase-uid")
            .email("user@test.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .phoneNumber("01012345678")
            .role(User.Role.USER)
            .status(User.Status.ACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();

    given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("Password123!", "hashed-password")).willReturn(true);
    given(jwtProvider.generateAccessToken(1L, "USER")).willReturn("access-token");
    given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token");
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.login(request) 호출");
    authService.login(request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then 1] resolveLoginFailureLogs(1L) 호출 확인");
    then(adminLogSaveService).should().resolveLoginFailureLogs(1L);
    System.out.println("        ✓ resolveLoginFailureLogs(userId=1) 호출됨 (실패 이력 해소)");

    System.out.println("[Then 2] saveSystemErrorLog() 미호출 확인");
    then(adminLogSaveService)
        .should(org.mockito.Mockito.never())
        .saveSystemErrorLog(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    System.out.println("        ✓ saveSystemErrorLog() 호출되지 않음 (에러 로그 미기록)");

    System.out.println("\n=== PASSED ===\n");
  }
}
