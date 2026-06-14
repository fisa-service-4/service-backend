package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.auth.dto.request.PinChangeRequest;
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
class AuthServiceChangePinTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private JwtProvider jwtProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private TransactionServerClient transactionServerClient;
  @Mock private AdminLogSaveService adminLogSaveService;

  @InjectMocks private AuthService authService;

  private PinChangeRequest pinRequest(String currentPin, String newPin) {
    PinChangeRequest request = new PinChangeRequest();
    ReflectionTestUtils.setField(request, "currentPin", currentPin);
    ReflectionTestUtils.setField(request, "newPin", newPin);
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
  // H-01. 정상 PIN 변경
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("H-01: 정상 PIN 변경 - changePin() 호출, pinHash 교체, failCount=0, lockedYn=false")
  void changePin_success() {
    System.out.println("\n=== H-01: 정상 PIN 변경 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PinAuth(lockedYn=false, failCount=2, pinHash=\"encoded-pin\")");
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

    System.out.println("[Given] passwordEncoder.encode(\"369147\") = \"encoded-369147\"");
    given(passwordEncoder.encode("369147")).willReturn("encoded-369147");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.changePin(1L, request) 호출");
    System.out.println("[Then 1] 예외 없이 정상 종료 확인");
    assertThatCode(() -> authService.changePin(1L, pinRequest("147258", "369147")))
        .doesNotThrowAnyException();
    System.out.println("        ✓ changePin() 예외 없이 정상 종료됨");

    System.out.println("[Then 2] pinAuth.changePin() 호출 확인 (pinHash 변경)");
    assertThat(pinAuth.getPinHash()).isEqualTo("encoded-369147");
    System.out.println("        ✓ pinHash = \"encoded-369147\" (encoded-pin → 교체됨)");

    System.out.println("[Then 3] changePin() 내부에서 failCount=0, lockedYn=false 리셋 확인");
    assertThat(pinAuth.getFailCount()).isZero();
    assertThat(pinAuth.getLockedYn()).isFalse();
    System.out.println("        ✓ failCount = 0 (이전 실패 이력 초기화됨)");
    System.out.println("        ✓ lockedYn = false");

    System.out.println("[Then 4] pinChangedAt 기록 확인");
    assertThat(pinAuth.getPinChangedAt()).isNotNull();
    System.out.println("        ✓ pinChangedAt 기록됨");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // H-02. 잠금 상태에서 변경 시도
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("H-02: 잠금 상태 변경 시도 - AUTH_009 예외 발생, passwordEncoder 미호출")
  void changePin_locked() {
    System.out.println("\n=== H-02: 잠금 상태에서 변경 시도 ===");

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
    System.out.println("\n[When] authService.changePin(1L, request) 호출");
    System.out.println("[Then 1] BusinessException(AUTH_009) 발생 확인");
    assertThatThrownBy(() -> authService.changePin(1L, pinRequest("147258", "369147")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_009);
    System.out.println("        ✓ BusinessException(AUTH_009) 발생 (PIN 잠금 상태)");

    System.out.println("[Then 2] passwordEncoder 미호출 확인");
    then(passwordEncoder).shouldHaveNoInteractions();
    System.out.println("        ✓ passwordEncoder 호출되지 않음 (잠금 체크에서 즉시 종료)");

    System.out.println("[Then 3] failCount 변화 없음 확인");
    assertThat(pinAuth.getFailCount()).isEqualTo(5);
    System.out.println("        ✓ failCount = 5 그대로 유지");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // H-03. 현재 PIN 불일치
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("H-03: 현재 PIN 불일치 - fail() 호출, AUTH_008 예외 발생")
  void changePin_currentPinMismatch() {
    System.out.println("\n=== H-03: 현재 PIN 불일치 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PinAuth(lockedYn=false, failCount=0)");
    PinAuth pinAuth =
        PinAuth.builder()
            .user(buildUser())
            .pinHash("encoded-pin")
            .failCount(0)
            .lockedYn(false)
            .build();
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.of(pinAuth));

    System.out.println("[Given] passwordEncoder.matches(\"wrongPin\", \"encoded-pin\") = false");
    given(passwordEncoder.matches("wrongPin", "encoded-pin")).willReturn(false);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.changePin(1L, request) 호출");
    System.out.println("[Then 1] BusinessException(AUTH_008) 발생 확인");
    assertThatThrownBy(() -> authService.changePin(1L, pinRequest("wrongPin", "369147")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_008);
    System.out.println("        ✓ BusinessException(AUTH_008) 발생 (현재 PIN 불일치)");

    System.out.println("[Then 2] fail() 호출 확인 (failCount 0 → 1)");
    assertThat(pinAuth.getFailCount()).isEqualTo(1);
    System.out.println("        ✓ failCount = 1 (실패 횟수 증가)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // H-04. 새 PIN 유효성 실패 (반복 숫자)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("H-04: 새 PIN 유효성 실패 (반복 숫자 \"999999\") - AUTH_010 예외, changePin() 미호출")
  void changePin_newPinInvalid() {
    System.out.println("\n=== H-04: 새 PIN 유효성 실패 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PinAuth(lockedYn=false, failCount=0, pinHash=\"encoded-pin\")");
    PinAuth pinAuth =
        PinAuth.builder()
            .user(buildUser())
            .pinHash("encoded-pin")
            .failCount(0)
            .lockedYn(false)
            .build();
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.of(pinAuth));

    System.out.println("[Given] passwordEncoder.matches(\"147258\", \"encoded-pin\") = true (현재 PIN 일치)");
    given(passwordEncoder.matches("147258", "encoded-pin")).willReturn(true);

    System.out.println("[Given] 새 PIN = \"999999\" (반복 숫자 — validatePinFormat 실패 예정)");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.changePin(1L, request) 호출");
    System.out.println("[Then 1] BusinessException(AUTH_010) 발생 확인");
    assertThatThrownBy(() -> authService.changePin(1L, pinRequest("147258", "999999")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_010);
    System.out.println("        ✓ BusinessException(AUTH_010) 발생 (반복 숫자 PIN 불가)");

    System.out.println("[Then 2] changePin() 미호출 확인 (pinHash 변경 없음)");
    assertThat(pinAuth.getPinHash()).isEqualTo("encoded-pin");
    System.out.println("        ✓ pinHash = \"encoded-pin\" 그대로 유지 (changePin() 호출 안 됨)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // H-05. PinAuth 없음
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("H-05: PinAuth 없음 - USER_001 예외 발생, passwordEncoder 미호출")
  void changePin_pinAuthNotFound() {
    System.out.println("\n=== H-05: PinAuth 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] pinAuthRepository.findByUserId(1L) = Optional.empty()");
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.changePin(1L, request) 호출");
    System.out.println("[Then] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> authService.changePin(1L, pinRequest("147258", "369147")))
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
