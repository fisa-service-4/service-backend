package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.auth.dto.response.TokenResponse;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceReissueTest {

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
  // D-01. 정상 재발급
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("D-01: 정상 재발급 - 새 토큰 발급 및 Redis 갱신 확인")
  void reissue_success() {
    System.out.println("\n=== D-01: 정상 재발급 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] validateRefreshToken() 정상 통과 (예외 없음)");
    doNothing().when(jwtProvider).validateRefreshToken("old-refresh-token");

    System.out.println("[Given] getUserId(\"old-refresh-token\") = 1L");
    given(jwtProvider.getUserId("old-refresh-token")).willReturn(1L);

    System.out.println("[Given] Redis \"refresh:1\" = \"old-refresh-token\" (일치)");
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("refresh:1")).willReturn("old-refresh-token");

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

    System.out.println("[Given] userRepository.findById(1L) = User 존재");
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    System.out.println("[Given] 새 토큰 반환값 설정");
    given(jwtProvider.generateAccessToken(1L, "USER")).willReturn("new-access-token");
    given(jwtProvider.generateRefreshToken(1L)).willReturn("new-refresh-token");
    doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.reissue(\"old-refresh-token\") 호출");
    TokenResponse response = authService.reissue("old-refresh-token");

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then 1] 새 토큰 생성 호출 확인");
    then(jwtProvider).should().generateAccessToken(1L, "USER");
    then(jwtProvider).should().generateRefreshToken(1L);
    System.out.println("        ✓ generateAccessToken(1L, \"USER\") 호출됨");
    System.out.println("        ✓ generateRefreshToken(1L) 호출됨");

    System.out.println("[Then 2] Redis \"refresh:1\" 새 토큰으로 갱신 확인 (TTL 7일)");
    then(valueOperations).should().set("refresh:1", "new-refresh-token", 7L, TimeUnit.DAYS);
    System.out.println("        ✓ set(\"refresh:1\", \"new-refresh-token\", 7, DAYS) 호출됨");

    System.out.println("[Then 3] TokenResponse 응답값 검증");
    assertThat(response.getAccessToken()).isEqualTo("new-access-token");
    assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    System.out.println("        ✓ accessToken = new-access-token");
    System.out.println("        ✓ refreshToken = new-refresh-token");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // D-02. JWT 서명/형식 오류
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("D-02: JWT 서명/형식 오류 - AUTH_005 예외 전파 및 Redis 조회 미호출")
  void reissue_invalidToken() {
    System.out.println("\n=== D-02: JWT 서명/형식 오류 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] validateRefreshToken(\"invalid-token\") → BusinessException(AUTH_005) 발생");
    willThrow(new BusinessException(ErrorCode.AUTH_005))
        .given(jwtProvider)
        .validateRefreshToken("invalid-token");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.reissue(\"invalid-token\") 호출");
    System.out.println("[Then] BusinessException(AUTH_005) 그대로 전파 확인");
    assertThatThrownBy(() -> authService.reissue("invalid-token"))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_005);
    System.out.println("       ✓ BusinessException(AUTH_005) 전파됨 (유효하지 않은 토큰)");

    System.out.println("[Then] Redis 조회 미호출 확인");
    then(redisTemplate).shouldHaveNoInteractions();
    System.out.println("       ✓ redisTemplate 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // D-03. Redis에 토큰 없음 (만료 또는 로그아웃 후 재시도)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("D-03: Redis 토큰 없음 - AUTH_005 예외 발생 (만료 또는 로그아웃 후 재시도)")
  void reissue_redisTokenNotFound() {
    System.out.println("\n=== D-03: Redis에 토큰 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] validateRefreshToken() 정상 통과");
    doNothing().when(jwtProvider).validateRefreshToken("valid-refresh-token");

    System.out.println("[Given] getUserId(\"valid-refresh-token\") = 1L");
    given(jwtProvider.getUserId("valid-refresh-token")).willReturn(1L);

    System.out.println("[Given] Redis.get(\"refresh:1\") = null (만료 또는 로그아웃으로 삭제된 상태)");
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("refresh:1")).willReturn(null);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.reissue(\"valid-refresh-token\") 호출");
    System.out.println("[Then] BusinessException(AUTH_005) 발생 확인");
    assertThatThrownBy(() -> authService.reissue("valid-refresh-token"))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_005);
    System.out.println("       ✓ BusinessException(AUTH_005) 발생 (유효하지 않은 토큰)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // D-04. Redis 토큰 불일치 (다른 기기에서 재발급 후 이전 토큰 재사용)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("D-04: Redis 토큰 불일치 - AUTH_005 예외 발생 (이전 토큰 재사용 차단)")
  void reissue_redisTokenMismatch() {
    System.out.println("\n=== D-04: Redis 토큰 불일치 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] validateRefreshToken() 정상 통과");
    doNothing().when(jwtProvider).validateRefreshToken("old-refresh-token");

    System.out.println("[Given] getUserId(\"old-refresh-token\") = 1L");
    given(jwtProvider.getUserId("old-refresh-token")).willReturn(1L);

    System.out.println("[Given] Redis.get(\"refresh:1\") = \"different-token\" (다른 기기에서 재발급된 토큰)");
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("refresh:1")).willReturn("different-token");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.reissue(\"old-refresh-token\") 호출");
    System.out.println("[Then] BusinessException(AUTH_005) 발생 확인");
    assertThatThrownBy(() -> authService.reissue("old-refresh-token"))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_005);
    System.out.println("       ✓ BusinessException(AUTH_005) 발생 (이전 토큰 재사용 차단)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // D-05. 사용자 없음
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("D-05: 사용자 없음 - USER_001 예외 발생 (JWT·Redis 통과 후 DB에서 사용자 미존재)")
  void reissue_userNotFound() {
    System.out.println("\n=== D-05: 사용자 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] validateRefreshToken() 정상 통과");
    doNothing().when(jwtProvider).validateRefreshToken("valid-refresh-token");

    System.out.println("[Given] getUserId(\"valid-refresh-token\") = 1L");
    given(jwtProvider.getUserId("valid-refresh-token")).willReturn(1L);

    System.out.println("[Given] Redis.get(\"refresh:1\") = \"valid-refresh-token\" (일치)");
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("refresh:1")).willReturn("valid-refresh-token");

    System.out.println("[Given] userRepository.findById(1L) = Optional.empty()");
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.reissue(\"valid-refresh-token\") 호출");
    System.out.println("[Then] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> authService.reissue("valid-refresh-token"))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
    System.out.println("       ✓ BusinessException(USER_001) 발생 (사용자를 찾을 수 없음)");

    System.out.println("\n=== PASSED ===\n");
  }
}
