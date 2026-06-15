package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.auth.dto.request.PinVerifyRequest;
import com.service.domain.auth.entity.PinAuth;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import com.service.global.security.JwtProvider;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceVerifyPinTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private JwtProvider jwtProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private TransactionServerClient transactionServerClient;
  @Mock private AdminLogSaveService adminLogSaveService;

  @InjectMocks private AuthService authService;

  private PinVerifyRequest pinRequest(String pin) {
    PinVerifyRequest request = new PinVerifyRequest();
    ReflectionTestUtils.setField(request, "pin", pin);
    return request;
  }

  private User buildUser() {
    return User.builder()
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
  }

  // ────────────────────────────────────────────────────────────────────────────
  // G-01. 정상 PIN 검증
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("G-01: 정상 PIN 검증 - resetFailCount() 호출, 예외 없음")
  void verifyPin_success() {
    System.out.println("\n=== G-01: 정상 PIN 검증 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PinAuth(lockedYn=false, failCount=2) — 이전 실패 이력 있음");
    PinAuth pinAuth =
        PinAuth.builder()
            .user(buildUser())
            .pinHash("encoded-pin")
            .failCount(2)
            .lockedYn(false)
            .build();
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.of(pinAuth));

    System.out.println("[Given] passwordEncoder.matches(\"147258\", \"encoded-pin\") = true");
    given(passwordEncoder.matches("147258", "encoded-pin")).willReturn(true);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.verifyPin(1L, request) 호출");
    System.out.println("[Then 1] 예외 없이 정상 종료 확인");
    assertThatCode(() -> authService.verifyPin(1L, pinRequest("147258")))
        .doesNotThrowAnyException();
    System.out.println("        ✓ verifyPin() 예외 없이 정상 종료됨");

    System.out.println("[Then 2] pinAuth.resetFailCount() 호출 확인 (failCount = 2 → 0)");
    assertThat(pinAuth.getFailCount()).isZero();
    System.out.println("        ✓ failCount = 0 (이전 실패 이력 초기화됨)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // G-02. 잠금 상태에서 검증 시도
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("G-02: 잠금 상태 검증 시도 - AUTH_009 예외 발생, fail() 미호출")
  void verifyPin_locked() {
    System.out.println("\n=== G-02: 잠금 상태에서 검증 시도 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PinAuth(lockedYn=true, failCount=5) — 이미 잠금 상태");
    PinAuth pinAuth =
        PinAuth.builder()
            .user(buildUser())
            .pinHash("encoded-pin")
            .failCount(5)
            .lockedYn(true)
            .build();
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.of(pinAuth));

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.verifyPin(1L, anyPin) 호출");
    System.out.println("[Then 1] BusinessException(AUTH_009) 발생 확인");
    assertThatThrownBy(() -> authService.verifyPin(1L, pinRequest("147258")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_009);
    System.out.println("        ✓ BusinessException(AUTH_009) 발생 (PIN 잠금 상태)");

    System.out.println("[Then 2] fail() 미호출 확인 (failCount 증가 없음)");
    assertThat(pinAuth.getFailCount()).isEqualTo(5);
    then(passwordEncoder).shouldHaveNoInteractions();
    System.out.println("        ✓ failCount = 5 그대로 유지 (잠금 상태 진입 시 PIN 비교 없이 즉시 예외)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // G-03. PIN 불일치 (1~4회 — 잠금 미발생)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("G-03: PIN 불일치 (failCount=3→4) - AUTH_008 예외, lockedYn=false 유지")
  void verifyPin_wrongPin_notYetLocked() {
    System.out.println("\n=== G-03: PIN 불일치 (1~4회 — 잠금 미발생) ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PinAuth(lockedYn=false, failCount=3) — 3회 실패 이력");
    PinAuth pinAuth =
        PinAuth.builder()
            .user(buildUser())
            .pinHash("encoded-pin")
            .failCount(3)
            .lockedYn(false)
            .build();
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.of(pinAuth));

    System.out.println("[Given] passwordEncoder.matches(\"wrongPin\", \"encoded-pin\") = false");
    given(passwordEncoder.matches("wrongPin", "encoded-pin")).willReturn(false);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.verifyPin(1L, \"wrongPin\") 호출");
    System.out.println("[Then 1] BusinessException(AUTH_008) 발생 확인");
    assertThatThrownBy(() -> authService.verifyPin(1L, pinRequest("wrongPin")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_008);
    System.out.println("        ✓ BusinessException(AUTH_008) 발생 (PIN 불일치)");

    System.out.println("[Then 2] fail() 호출 확인 (failCount 3 → 4)");
    assertThat(pinAuth.getFailCount()).isEqualTo(4);
    System.out.println("        ✓ failCount = 4 (실패 횟수 증가)");

    System.out.println("[Then 3] lockedYn = false 유지 (5회 미만이므로 잠금 없음)");
    assertThat(pinAuth.getLockedYn()).isFalse();
    System.out.println("        ✓ lockedYn = false (4회 실패 — 잠금 미발생)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // G-04. PIN 5회 실패 → 잠금 처리
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("G-04: PIN 5회 실패 → 잠금 - failCount=5, lockedYn=true, user LOCKED, AUTH_009")
  void verifyPin_wrongPin_locked() {
    System.out.println("\n=== G-04: PIN 5회 실패 → 잠금 처리 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PinAuth(lockedYn=false, failCount=4) — 4회 실패, 1회 남음");
    User user = buildUser();
    PinAuth pinAuth =
        PinAuth.builder()
            .user(user)
            .pinHash("encoded-pin")
            .failCount(4)
            .lockedYn(false)
            .build();
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.of(pinAuth));

    System.out.println("[Given] passwordEncoder.matches(\"wrongPin\", \"encoded-pin\") = false");
    given(passwordEncoder.matches("wrongPin", "encoded-pin")).willReturn(false);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.verifyPin(1L, \"wrongPin\") 호출");
    System.out.println("[Then 1] BusinessException(AUTH_009) 발생 확인 (AUTH_008 아님 — 5회 실패 즉시 잠금)");
    assertThatThrownBy(() -> authService.verifyPin(1L, pinRequest("wrongPin")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_009);
    System.out.println("        ✓ BusinessException(AUTH_009) 발생 (PIN 잠금 처리)");

    System.out.println("[Then 2] fail() 호출 확인 (failCount 4 → 5, lockedYn = true)");
    assertThat(pinAuth.getFailCount()).isEqualTo(5);
    assertThat(pinAuth.getLockedYn()).isTrue();
    assertThat(pinAuth.getLockedAt()).isNotNull();
    System.out.println("        ✓ failCount = 5");
    System.out.println("        ✓ lockedYn = true (5회 달성 → 즉시 잠금)");
    System.out.println("        ✓ lockedAt 기록됨");

    System.out.println("[Then 3] user.updateStatus(LOCKED) 호출 확인");
    assertThat(user.getStatus()).isEqualTo(User.Status.LOCKED);
    System.out.println("        ✓ user.status = LOCKED (PinAuth 잠금 + User 상태 동기화)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // G-05. PinAuth 없음
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("G-05: PinAuth 없음 - USER_001 예외 발생")
  void verifyPin_pinAuthNotFound() {
    System.out.println("\n=== G-05: PinAuth 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] pinAuthRepository.findByUserId(1L) = Optional.empty()");
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.verifyPin(1L, request) 호출");
    System.out.println("[Then] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> authService.verifyPin(1L, pinRequest("147258")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
    System.out.println("       ✓ BusinessException(USER_001) 발생 (PIN 미등록 사용자)");

    System.out.println("[Then] passwordEncoder 미호출 확인");
    then(passwordEncoder).shouldHaveNoInteractions();
    System.out.println("       ✓ passwordEncoder 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }
}
