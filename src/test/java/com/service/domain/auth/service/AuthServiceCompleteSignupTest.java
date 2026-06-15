package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.security.JwtProvider;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceCompleteSignupTest {

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
  // E-01. Firebase 정상 생성
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("E-01: Firebase 정상 생성 - uid 업데이트, linkUser 호출, activate() 확인")
  void completeSignup_firebaseSuccess() throws Exception {
    System.out.println("\n=== E-01: Firebase 정상 생성 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = User 존재 (status=INACTIVE)");
    User user =
        User.builder()
            .userId(1L)
            .firebaseUid("temp-uid")
            .email("user@test.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .phoneNumber("01012345678")
            .role(User.Role.USER)
            .status(User.Status.INACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
    UserRecord mockUserRecord = mock(UserRecord.class);

    System.out.println("[Given] FirebaseAuth.createUser() → UserRecord(uid=\"firebase-uid-123\") 반환");
    given(mockUserRecord.getUid()).willReturn("firebase-uid-123");
    given(mockFirebaseAuth.createUser(any(UserRecord.CreateRequest.class)))
        .willReturn(mockUserRecord);

    // ── When ───────────────────────────────────────────────────
    try (MockedStatic<FirebaseAuth> mockedStatic = mockStatic(FirebaseAuth.class)) {
      mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

      System.out.println("\n[When] authService.completeSignup(1L) 호출");
      authService.completeSignup(1L);

      // ── Then ─────────────────────────────────────────────────
      System.out.println("\n[Then 1] user.updateFirebaseUid(\"firebase-uid-123\") 호출 확인");
      assertThat(user.getFirebaseUid()).isEqualTo("firebase-uid-123");
      System.out.println("        ✓ firebaseUid = \"firebase-uid-123\" (temp-uid에서 교체됨)");

      System.out.println("[Then 2] transactionServerClient.linkUser() 호출 확인");
      then(transactionServerClient)
          .should()
          .linkUser(1L, "firebase-uid-123", "홍길동", "01012345678");
      System.out.println("        ✓ linkUser(userId=1, uid=\"firebase-uid-123\", name=\"홍길동\", phone=\"01012345678\") 호출됨");

      System.out.println("[Then 3] user.activate() 호출 확인 (status = ACTIVE)");
      assertThat(user.getStatus()).isEqualTo(User.Status.ACTIVE);
      System.out.println("        ✓ status = ACTIVE (INACTIVE → ACTIVE)");
    }

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // E-02. Firebase createUser 실패 → getUserByEmail 폴백 성공
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("E-02: createUser 실패 → getUserByEmail 폴백 성공 - 폴백 uid 업데이트, activate() 확인")
  void completeSignup_firebaseCreateUserFails_fallbackSuccess() throws Exception {
    System.out.println("\n=== E-02: Firebase createUser 실패 → getUserByEmail 폴백 성공 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = User 존재 (status=INACTIVE)");
    User user =
        User.builder()
            .userId(1L)
            .firebaseUid("temp-uid")
            .email("user@test.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .phoneNumber("01012345678")
            .role(User.Role.USER)
            .status(User.Status.INACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
    UserRecord mockUserRecord = mock(UserRecord.class);

    System.out.println("[Given] FirebaseAuth.createUser() → FirebaseAuthException 발생 (이미 존재하는 이메일)");
    given(mockFirebaseAuth.createUser(any(UserRecord.CreateRequest.class)))
        .willThrow(mock(FirebaseAuthException.class));

    System.out.println("[Given] FirebaseAuth.getUserByEmail(\"user@test.com\") → UserRecord(uid=\"existing-uid\") 반환");
    given(mockFirebaseAuth.getUserByEmail("user@test.com")).willReturn(mockUserRecord);
    given(mockUserRecord.getUid()).willReturn("existing-uid");

    // ── When ───────────────────────────────────────────────────
    try (MockedStatic<FirebaseAuth> mockedStatic = mockStatic(FirebaseAuth.class)) {
      mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

      System.out.println("\n[When] authService.completeSignup(1L) 호출");
      authService.completeSignup(1L);

      // ── Then ─────────────────────────────────────────────────
      System.out.println("\n[Then 1] 폴백 uid(\"existing-uid\")로 업데이트 확인");
      assertThat(user.getFirebaseUid()).isEqualTo("existing-uid");
      System.out.println("        ✓ firebaseUid = \"existing-uid\" (createUser 실패 → getUserByEmail 폴백 uid 사용)");

      System.out.println("[Then 2] transactionServerClient.linkUser() 호출 확인 (폴백 uid로 연동)");
      then(transactionServerClient)
          .should()
          .linkUser(1L, "existing-uid", "홍길동", "01012345678");
      System.out.println("        ✓ linkUser(userId=1, uid=\"existing-uid\", name=\"홍길동\", phone=\"01012345678\") 호출됨");

      System.out.println("[Then 3] user.activate() 호출 확인 (Firebase 폴백 성공 후에도 항상 ACTIVE)");
      assertThat(user.getStatus()).isEqualTo(User.Status.ACTIVE);
      System.out.println("        ✓ status = ACTIVE (INACTIVE → ACTIVE)");
    }

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // E-03. Firebase 전체 실패 (createUser + getUserByEmail 모두 실패)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("E-03: Firebase 전체 실패 - uid 미업데이트, linkUser 미호출, activate() 호출 확인")
  void completeSignup_firebaseAllFails() throws Exception {
    System.out.println("\n=== E-03: Firebase 전체 실패 (createUser + getUserByEmail 모두 실패) ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = User 존재 (status=INACTIVE, firebaseUid=\"temp-uid\")");
    User user =
        User.builder()
            .userId(1L)
            .firebaseUid("temp-uid")
            .email("user@test.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .phoneNumber("01012345678")
            .role(User.Role.USER)
            .status(User.Status.INACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);

    System.out.println("[Given] FirebaseAuth.createUser() → FirebaseAuthException 발생");
    given(mockFirebaseAuth.createUser(any(UserRecord.CreateRequest.class)))
        .willThrow(mock(FirebaseAuthException.class));

    System.out.println("[Given] FirebaseAuth.getUserByEmail() → FirebaseAuthException 발생 (폴백도 실패)");
    given(mockFirebaseAuth.getUserByEmail("user@test.com"))
        .willThrow(mock(FirebaseAuthException.class));

    // ── When ───────────────────────────────────────────────────
    try (MockedStatic<FirebaseAuth> mockedStatic = mockStatic(FirebaseAuth.class)) {
      mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

      System.out.println("\n[When] authService.completeSignup(1L) 호출");
      authService.completeSignup(1L);

      // ── Then ─────────────────────────────────────────────────
      System.out.println("\n[Then 1] user.updateFirebaseUid() 미호출 확인 (기존 임시값 유지)");
      assertThat(user.getFirebaseUid()).isEqualTo("temp-uid");
      System.out.println("        ✓ firebaseUid = \"temp-uid\" (Firebase 실패로 uid 업데이트되지 않음)");

      System.out.println("[Then 2] transactionServerClient.linkUser() 미호출 확인");
      then(transactionServerClient).shouldHaveNoInteractions();
      System.out.println("        ✓ linkUser() 호출되지 않음 (uid 없이 연동 불가)");

      System.out.println("[Then 3] user.activate() 호출 확인 (Firebase 실패와 무관하게 항상 ACTIVE)");
      assertThat(user.getStatus()).isEqualTo(User.Status.ACTIVE);
      System.out.println("        ✓ status = ACTIVE (Firebase 전체 실패에도 불구하고 계정 활성화)");
    }

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // E-04. transactionServerClient.linkUser 실패
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("E-04: linkUser 실패 - 예외 무시, uid 업데이트 유지, activate() 호출 확인")
  void completeSignup_linkUserFails() throws Exception {
    System.out.println("\n=== E-04: transactionServerClient.linkUser 실패 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = User 존재 (status=INACTIVE)");
    User user =
        User.builder()
            .userId(1L)
            .firebaseUid("temp-uid")
            .email("user@test.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .phoneNumber("01012345678")
            .role(User.Role.USER)
            .status(User.Status.INACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
    UserRecord mockUserRecord = mock(UserRecord.class);

    System.out.println("[Given] FirebaseAuth.createUser() → UserRecord(uid=\"firebase-uid-123\") 반환 (Firebase 정상)");
    given(mockUserRecord.getUid()).willReturn("firebase-uid-123");
    given(mockFirebaseAuth.createUser(any(UserRecord.CreateRequest.class)))
        .willReturn(mockUserRecord);

    System.out.println("[Given] transactionServerClient.linkUser() → RuntimeException 발생");
    willThrow(new RuntimeException("transaction-server 연동 실패"))
        .given(transactionServerClient)
        .linkUser(anyLong(), anyString(), anyString(), anyString());

    // ── When & Then ────────────────────────────────────────────
    try (MockedStatic<FirebaseAuth> mockedStatic = mockStatic(FirebaseAuth.class)) {
      mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

      System.out.println("\n[When] authService.completeSignup(1L) 호출");
      System.out.println("[Then 1] 예외 없이 정상 종료 확인 (linkUser 실패는 비치명적 — catch로 무시)");
      assertThatCode(() -> authService.completeSignup(1L)).doesNotThrowAnyException();
      System.out.println("        ✓ completeSignup() 예외 없이 정상 종료됨");

      System.out.println("[Then 2] user.updateFirebaseUid(\"firebase-uid-123\") 호출 확인");
      assertThat(user.getFirebaseUid()).isEqualTo("firebase-uid-123");
      System.out.println("        ✓ firebaseUid = \"firebase-uid-123\" (Firebase는 성공했으므로 uid 업데이트됨)");

      System.out.println("[Then 3] user.activate() 호출 확인 (linkUser 실패와 무관하게 항상 ACTIVE)");
      assertThat(user.getStatus()).isEqualTo(User.Status.ACTIVE);
      System.out.println("        ✓ status = ACTIVE (linkUser 실패에도 불구하고 계정 활성화)");
    }

    System.out.println("\n=== PASSED ===\n");
  }
}
