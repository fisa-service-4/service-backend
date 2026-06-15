package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.auth.dto.request.PinRegisterRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterPinTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private JwtProvider jwtProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private TransactionServerClient transactionServerClient;
  @Mock private AdminLogSaveService adminLogSaveService;

  @InjectMocks private AuthService authService;

  private PinRegisterRequest pinRequest(String pin) {
    PinRegisterRequest request = new PinRegisterRequest();
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
  // F-01. 최초 PIN 등록
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("F-01: 최초 PIN 등록 - PinAuth 신규 저장, failCount=0, lockedYn=false 확인")
  void registerPin_firstTime() {
    System.out.println("\n=== F-01: 최초 PIN 등록 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 유효한 PIN \"147258\"");
    PinRegisterRequest request = pinRequest("147258");

    System.out.println("[Given] userRepository.findById(1L) = User 존재");
    given(userRepository.findById(1L)).willReturn(Optional.of(buildUser()));

    System.out.println("[Given] pinAuthRepository.existsByUserId(1L) = false (최초 등록)");
    given(pinAuthRepository.existsByUserId(1L)).willReturn(false);

    System.out.println("[Given] passwordEncoder.encode(\"147258\") = \"encoded-147258\"");
    given(passwordEncoder.encode("147258")).willReturn("encoded-147258");

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.registerPin(1L, request) 호출");
    authService.registerPin(1L, request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then 1] passwordEncoder.encode(\"147258\") 호출 확인");
    then(passwordEncoder).should().encode("147258");
    System.out.println("        ✓ encode(\"147258\") 호출됨");

    System.out.println("[Then 2] pinAuthRepository.save() 호출 및 저장된 PinAuth 필드 검증");
    ArgumentCaptor<PinAuth> captor = ArgumentCaptor.forClass(PinAuth.class);
    then(pinAuthRepository).should().save(captor.capture());
    PinAuth saved = captor.getValue();

    assertThat(saved.getPinHash()).isEqualTo("encoded-147258");
    assertThat(saved.getFailCount()).isZero();
    assertThat(saved.getLockedYn()).isFalse();
    System.out.println("        ✓ pinHash = \"encoded-147258\"");
    System.out.println("        ✓ failCount = 0");
    System.out.println("        ✓ lockedYn = false");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // F-02. PIN 재등록 (이미 등록된 경우 — upsert)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("F-02: PIN 재등록 - changePin() 호출, 신규 save() 미호출 확인")
  void registerPin_reRegister() {
    System.out.println("\n=== F-02: PIN 재등록 (upsert) ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 유효한 새 PIN \"258369\"");
    PinRegisterRequest request = pinRequest("258369");

    System.out.println("[Given] userRepository.findById(1L) = User 존재");
    given(userRepository.findById(1L)).willReturn(Optional.of(buildUser()));

    System.out.println("[Given] pinAuthRepository.existsByUserId(1L) = true (이미 등록됨)");
    given(pinAuthRepository.existsByUserId(1L)).willReturn(true);

    PinAuth existingPinAuth =
        PinAuth.builder()
            .user(buildUser())
            .pinHash("old-encoded-pin")
            .failCount(0)
            .lockedYn(false)
            .build();

    System.out.println("[Given] pinAuthRepository.findByUserId(1L) = 기존 PinAuth(pinHash=\"old-encoded-pin\")");
    given(pinAuthRepository.findByUserId(1L)).willReturn(Optional.of(existingPinAuth));

    System.out.println("[Given] passwordEncoder.encode(\"258369\") = \"encoded-258369\"");
    given(passwordEncoder.encode("258369")).willReturn("encoded-258369");

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.registerPin(1L, request) 호출");
    authService.registerPin(1L, request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then 1] 기존 PinAuth.changePin() 호출 확인 (pinHash 변경됨)");
    assertThat(existingPinAuth.getPinHash()).isEqualTo("encoded-258369");
    assertThat(existingPinAuth.getFailCount()).isZero();
    assertThat(existingPinAuth.getLockedYn()).isFalse();
    System.out.println("        ✓ pinHash = \"encoded-258369\" (old-encoded-pin → 교체됨)");
    System.out.println("        ✓ failCount = 0 (changePin() 내부에서 리셋)");
    System.out.println("        ✓ lockedYn = false (changePin() 내부에서 리셋)");

    System.out.println("[Then 2] pinAuthRepository.save() 미호출 확인 (새 엔티티 생성 안 함)");
    then(pinAuthRepository).should(never()).save(any(PinAuth.class));
    System.out.println("        ✓ save() 호출되지 않음 (기존 엔티티 수정만)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // F-03. 6자리 숫자 아닌 PIN (짧은 PIN)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("F-03: 짧은 PIN (4자리) - AUTH_010 예외 발생, 저장 미호출")
  void registerPin_tooShort() {
    System.out.println("\n=== F-03: 6자리 숫자 아닌 PIN (짧은 PIN) ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PIN = \"1234\" (4자리 — 6자리 숫자 형식 불충족)");
    PinRegisterRequest request = pinRequest("1234");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.registerPin(1L, request) 호출");
    System.out.println("[Then] BusinessException(AUTH_010) 발생 확인");
    assertThatThrownBy(() -> authService.registerPin(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_010);
    System.out.println("       ✓ BusinessException(AUTH_010) 발생 (연속/반복 숫자 PIN 불가)");

    System.out.println("[Then] pinAuthRepository.save() 미호출 확인");
    then(pinAuthRepository).shouldHaveNoInteractions();
    System.out.println("       ✓ pinAuthRepository 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // F-04. 모두 같은 숫자 PIN
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("F-04: 반복 숫자 PIN (111111) - AUTH_010 예외 발생")
  void registerPin_allSameDigit() {
    System.out.println("\n=== F-04: 모두 같은 숫자 PIN ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PIN = \"111111\" (모두 같은 숫자)");
    PinRegisterRequest request = pinRequest("111111");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.registerPin(1L, request) 호출");
    System.out.println("[Then] BusinessException(AUTH_010) 발생 확인");
    assertThatThrownBy(() -> authService.registerPin(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_010);
    System.out.println("       ✓ BusinessException(AUTH_010) 발생 (반복 숫자 PIN 불가)");

    System.out.println("[Then] pinAuthRepository.save() 미호출 확인");
    then(pinAuthRepository).shouldHaveNoInteractions();
    System.out.println("       ✓ pinAuthRepository 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // F-05. 연속 오름차순 PIN
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("F-05: 연속 오름차순 PIN (123456) - AUTH_010 예외 발생")
  void registerPin_ascendingSequential() {
    System.out.println("\n=== F-05: 연속 오름차순 PIN ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PIN = \"123456\" (연속 오름차순)");
    PinRegisterRequest request = pinRequest("123456");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.registerPin(1L, request) 호출");
    System.out.println("[Then] BusinessException(AUTH_010) 발생 확인");
    assertThatThrownBy(() -> authService.registerPin(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_010);
    System.out.println("       ✓ BusinessException(AUTH_010) 발생 (연속 오름차순 PIN 불가)");

    System.out.println("[Then] pinAuthRepository.save() 미호출 확인");
    then(pinAuthRepository).shouldHaveNoInteractions();
    System.out.println("       ✓ pinAuthRepository 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // F-06. 연속 내림차순 PIN
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("F-06: 연속 내림차순 PIN (654321) - AUTH_010 예외 발생")
  void registerPin_descendingSequential() {
    System.out.println("\n=== F-06: 연속 내림차순 PIN ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] PIN = \"654321\" (연속 내림차순)");
    PinRegisterRequest request = pinRequest("654321");

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.registerPin(1L, request) 호출");
    System.out.println("[Then] BusinessException(AUTH_010) 발생 확인");
    assertThatThrownBy(() -> authService.registerPin(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_010);
    System.out.println("       ✓ BusinessException(AUTH_010) 발생 (연속 내림차순 PIN 불가)");

    System.out.println("[Then] pinAuthRepository.save() 미호출 확인");
    then(pinAuthRepository).shouldHaveNoInteractions();
    System.out.println("       ✓ pinAuthRepository 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // F-07. 사용자 없음
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("F-07: 사용자 없음 - USER_001 예외 발생, save() 미호출")
  void registerPin_userNotFound() {
    System.out.println("\n=== F-07: 사용자 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 유효한 PIN \"147258\" (형식 검증 통과)");
    PinRegisterRequest request = pinRequest("147258");

    System.out.println("[Given] userRepository.findById(1L) = Optional.empty()");
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.registerPin(1L, request) 호출");
    System.out.println("[Then] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> authService.registerPin(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
    System.out.println("       ✓ BusinessException(USER_001) 발생 (사용자를 찾을 수 없음)");

    System.out.println("[Then] pinAuthRepository.save() 미호출 확인");
    then(pinAuthRepository).should(never()).save(any(PinAuth.class));
    System.out.println("       ✓ save() 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }
}
