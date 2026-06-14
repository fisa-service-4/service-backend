package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceLogoutTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private JwtProvider jwtProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private TransactionServerClient transactionServerClient;
  @Mock private AdminLogSaveService adminLogSaveService;

  @InjectMocks private AuthService authService;

  // ────────────────────────────────────────────────────────────────────────────
  // C-01. 정상 로그아웃
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("C-01: 정상 로그아웃 - Redis refresh 토큰 삭제 확인")
  void logout_success() {
    System.out.println("\n=== C-01: 정상 로그아웃 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] Redis에 \"refresh:1\" 키로 refresh token 존재 (userId=1)");

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.logout(1L) 호출");
    authService.logout(1L);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] redisTemplate.delete(\"refresh:1\") 호출 확인");
    then(redisTemplate).should().delete("refresh:1");
    System.out.println("       ✓ redisTemplate.delete(\"refresh:1\") 호출됨");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // C-02. Redis에 토큰 없는 경우
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("C-02: Redis에 토큰 없는 경우 - 예외 없이 정상 종료 및 delete() 호출 확인")
  void logout_tokenNotExistsInRedis() {
    System.out.println("\n=== C-02: Redis에 토큰 없는 경우 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] Redis 빈 상태 (\"refresh:1\" 키 없음, 별도 stub 없음)");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.logout(1L) 호출");
    System.out.println("[Then] 예외 없이 정상 종료 확인");
    assertThatCode(() -> authService.logout(1L)).doesNotThrowAnyException();
    System.out.println("       ✓ 예외 발생 없이 정상 종료 (null safe)");

    System.out.println("[Then] redisTemplate.delete(\"refresh:1\") 호출 확인");
    then(redisTemplate).should().delete("refresh:1");
    System.out.println("       ✓ redisTemplate.delete(\"refresh:1\") 호출됨");

    System.out.println("\n=== PASSED ===\n");
  }
}
