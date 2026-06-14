package com.service.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import com.service.domain.auth.dto.request.SignupRequest;
import com.service.domain.auth.dto.response.SignupResponse;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignupTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private JwtProvider jwtProvider;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private TransactionServerClient transactionServerClient;
  @Mock private AdminLogSaveService adminLogSaveService;

  @Spy
  @InjectMocks
  private AuthService authService;

  // ────────────────────────────────────────────────────────────────────────────
  // A-01. 정상 회원가입
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("A-01: 정상 회원가입 - User/UserProfile 저장 및 completeSignup 호출")
  void signup_success() {
    System.out.println("\n=== A-01: 정상 회원가입 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 유효한 SignupRequest 생성");
    SignupRequest request = new SignupRequest();
    ReflectionTestUtils.setField(request, "email", "user@test.com");
    ReflectionTestUtils.setField(request, "password", "Password123!");
    ReflectionTestUtils.setField(request, "userName", "홍길동");
    ReflectionTestUtils.setField(request, "phoneNumber", "01012345678");
    ReflectionTestUtils.setField(request, "freelancerYn", true);
    ReflectionTestUtils.setField(request, "jobType", "DEVELOPER");
    ReflectionTestUtils.setField(request, "termsConsentYn", true);

    System.out.println("[Given] 이메일/전화번호 중복 없음");
    given(userRepository.existsByEmail("user@test.com")).willReturn(false);
    given(userRepository.existsByPhoneNumber("01012345678")).willReturn(false);

    System.out.println("[Given] 비밀번호 해시 반환값 설정");
    given(passwordEncoder.encode("Password123!")).willReturn("encoded-password");

    System.out.println("[Given] userRepository.save() 반환값 설정 (userId=1L, status=INACTIVE)");
    User savedUser = User.builder()
        .userId(1L)
        .firebaseUid("temp-uid")
        .email("user@test.com")
        .passwordHash("encoded-password")
        .userName("홍길동")
        .phoneNumber("01012345678")
        .role(User.Role.USER)
        .status(User.Status.INACTIVE)
        .notificationConsentYn(false)
        .termsConsentYn(true)
        .mydataConsentYn(false)
        .build();
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    System.out.println("[Given] completeSignup() stub (E-01~E-04에서 별도 검증)");
    doNothing().when(authService).completeSignup(anyLong());

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.signup(request) 호출");
    SignupResponse response = authService.signup(request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then 1] 이메일/전화번호 중복 체크 호출 확인");
    then(userRepository).should().existsByEmail("user@test.com");
    then(userRepository).should().existsByPhoneNumber("01012345678");
    System.out.println("        ✓ existsByEmail(\"user@test.com\") 호출됨");
    System.out.println("        ✓ existsByPhoneNumber(\"01012345678\") 호출됨");

    System.out.println("[Then 2] 비밀번호 해시화 확인");
    then(passwordEncoder).should().encode("Password123!");
    System.out.println("        ✓ passwordEncoder.encode(\"Password123!\") 호출됨");

    System.out.println("[Then 3] 저장된 User 필드 검증");
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    then(userRepository).should().save(userCaptor.capture());
    User capturedUser = userCaptor.getValue();

    assertThat(capturedUser.getStatus()).isEqualTo(User.Status.INACTIVE);
    assertThat(capturedUser.getRole()).isEqualTo(User.Role.USER);
    assertThat(capturedUser.getPasswordHash()).isEqualTo("encoded-password");
    assertThat(capturedUser.getNotificationConsentYn()).isFalse();
    assertThat(capturedUser.getMydataConsentYn()).isFalse();
    assertThat(capturedUser.getTermsConsentYn()).isTrue();
    System.out.println("        ✓ status = INACTIVE");
    System.out.println("        ✓ role = USER");
    System.out.println("        ✓ passwordHash = encoded-password (평문 아님)");
    System.out.println("        ✓ notificationConsentYn = false");
    System.out.println("        ✓ mydataConsentYn = false");
    System.out.println("        ✓ termsConsentYn = true");

    System.out.println("[Then 4] UserProfile 저장 검증");
    ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
    then(userProfileRepository).should().save(profileCaptor.capture());
    UserProfile capturedProfile = profileCaptor.getValue();

    assertThat(capturedProfile.getFreelancerYn()).isTrue();
    assertThat(capturedProfile.getJobType()).isEqualTo("DEVELOPER");
    System.out.println("        ✓ freelancerYn = true");
    System.out.println("        ✓ jobType = DEVELOPER");

    System.out.println("[Then 5] completeSignup(1L) 호출 확인");
    then(authService).should().completeSignup(1L);
    System.out.println("        ✓ completeSignup(userId=1L) 호출됨");

    System.out.println("[Then 6] SignupResponse 응답값 검증");
    assertThat(response.getUserId()).isEqualTo(1L);
    assertThat(response.getEmail()).isEqualTo("user@test.com");
    assertThat(response.getUserName()).isEqualTo("홍길동");
    System.out.println("        ✓ userId = 1");
    System.out.println("        ✓ email = user@test.com");
    System.out.println("        ✓ userName = 홍길동");
    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // A-02. 이메일 중복
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("A-02: 이메일 중복 - AUTH_001 예외 발생 및 save/completeSignup 미호출")
  void signup_duplicateEmail() {
    System.out.println("\n=== A-02: 이메일 중복 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 이미 가입된 이메일로 요청");
    SignupRequest request = new SignupRequest();
    ReflectionTestUtils.setField(request, "email", "user@test.com");
    ReflectionTestUtils.setField(request, "password", "Password123!");
    ReflectionTestUtils.setField(request, "userName", "홍길동");
    ReflectionTestUtils.setField(request, "phoneNumber", "01012345678");
    ReflectionTestUtils.setField(request, "freelancerYn", true);
    ReflectionTestUtils.setField(request, "termsConsentYn", true);

    System.out.println("[Given] existsByEmail(\"user@test.com\") = true (중복)");
    given(userRepository.existsByEmail("user@test.com")).willReturn(true);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.signup(request) 호출");
    System.out.println("[Then] BusinessException(AUTH_001) 발생 확인");
    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_001);
    System.out.println("       ✓ BusinessException 발생");
    System.out.println("       ✓ ErrorCode = AUTH_001 (이미 가입된 이메일)");

    System.out.println("[Then] userRepository.save() 미호출 확인");
    then(userRepository).should(org.mockito.Mockito.never()).save(any(User.class));
    System.out.println("       ✓ save() 호출되지 않음");

    System.out.println("[Then] completeSignup() 미호출 확인");
    then(authService).should(org.mockito.Mockito.never()).completeSignup(anyLong());
    System.out.println("       ✓ completeSignup() 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // A-03. 전화번호 중복
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("A-03: 전화번호 중복 - AUTH_002 예외 발생 및 save() 미호출")
  void signup_duplicatePhoneNumber() {
    System.out.println("\n=== A-03: 전화번호 중복 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 유효한 SignupRequest 생성");
    SignupRequest request = new SignupRequest();
    ReflectionTestUtils.setField(request, "email", "user@test.com");
    ReflectionTestUtils.setField(request, "password", "Password123!");
    ReflectionTestUtils.setField(request, "userName", "홍길동");
    ReflectionTestUtils.setField(request, "phoneNumber", "01012345678");
    ReflectionTestUtils.setField(request, "freelancerYn", true);
    ReflectionTestUtils.setField(request, "termsConsentYn", true);

    System.out.println("[Given] existsByEmail(\"user@test.com\") = false (이메일 중복 없음)");
    given(userRepository.existsByEmail("user@test.com")).willReturn(false);

    System.out.println("[Given] existsByPhoneNumber(\"01012345678\") = true (중복)");
    given(userRepository.existsByPhoneNumber("01012345678")).willReturn(true);

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] authService.signup(request) 호출");
    System.out.println("[Then] BusinessException(AUTH_002) 발생 확인");
    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_002);
    System.out.println("       ✓ BusinessException 발생");
    System.out.println("       ✓ ErrorCode = AUTH_002 (이미 가입된 전화번호)");

    System.out.println("[Then] userRepository.save() 미호출 확인");
    then(userRepository).should(org.mockito.Mockito.never()).save(any(User.class));
    System.out.println("       ✓ save() 호출되지 않음");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // A-04. 비밀번호 해시 저장 확인
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("A-04: 비밀번호 해시 저장 - encode() 호출 및 평문 저장 금지 확인")
  void signup_passwordIsHashed() {
    System.out.println("\n=== A-04: 비밀번호 해시 저장 확인 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] 정상 SignupRequest 생성 (rawPassword = \"Password1!\")");
    SignupRequest request = new SignupRequest();
    ReflectionTestUtils.setField(request, "email", "user@test.com");
    ReflectionTestUtils.setField(request, "password", "Password1!");
    ReflectionTestUtils.setField(request, "userName", "홍길동");
    ReflectionTestUtils.setField(request, "phoneNumber", "01012345678");
    ReflectionTestUtils.setField(request, "freelancerYn", true);
    ReflectionTestUtils.setField(request, "termsConsentYn", true);

    given(userRepository.existsByEmail("user@test.com")).willReturn(false);
    given(userRepository.existsByPhoneNumber("01012345678")).willReturn(false);

    System.out.println("[Given] passwordEncoder.encode(\"Password1!\") → \"hashed-password\" 반환");
    given(passwordEncoder.encode("Password1!")).willReturn("hashed-password");

    User savedUser =
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
    given(userRepository.save(any(User.class))).willReturn(savedUser);
    doNothing().when(authService).completeSignup(anyLong());

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] authService.signup(request) 호출");
    authService.signup(request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] passwordEncoder.encode(\"Password1!\") 호출 확인");
    then(passwordEncoder).should().encode("Password1!");
    System.out.println("       ✓ encode(\"Password1!\") 호출됨");

    System.out.println("[Then] 저장된 User의 passwordHash 검증");
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    then(userRepository).should().save(userCaptor.capture());
    User captured = userCaptor.getValue();

    assertThat(captured.getPasswordHash()).isNotEqualTo("Password1!");
    assertThat(captured.getPasswordHash()).isEqualTo("hashed-password");
    System.out.println("       ✓ passwordHash ≠ \"Password1!\" (평문 저장 금지)");
    System.out.println("       ✓ passwordHash = \"hashed-password\" (해시값 저장)");

    System.out.println("\n=== PASSED ===\n");
  }
}
