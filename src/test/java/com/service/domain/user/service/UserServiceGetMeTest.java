package com.service.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.user.dto.response.UserResponse;
import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceGetMeTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;

  @InjectMocks private UserService userService;

  private User buildUser() {
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
            .notificationConsentYn(true)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();
    ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 5, 1, 10, 0, 0));
    return user;
  }

  private UserProfile buildProfile(User user) {
    return UserProfile.builder()
        .user(user)
        .freelancerYn(true)
        .jobType("DEVELOPER")
        .build();
  }

  // ────────────────────────────────────────────────────────────────────────────
  // I-01. 정상 조회
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("I-01: 정상 조회 - UserResponse 반환, 모든 필드 검증")
  void getMe_success() {
    System.out.println("\n=== I-01: 정상 조회 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = User 존재");
    User user = buildUser();
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    System.out.println("[Given] userProfileRepository.findByUserId(1L) = UserProfile 존재");
    UserProfile profile = buildProfile(user);
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] userService.getMe(1L) 호출");
    UserResponse response = userService.getMe(1L);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] UserResponse 필드 검증");
    assertThat(response.getUserId()).isEqualTo(1L);
    assertThat(response.getEmail()).isEqualTo("user@test.com");
    assertThat(response.getUserName()).isEqualTo("홍길동");
    assertThat(response.getPhoneNumber()).isEqualTo("01012345678");
    assertThat(response.getRole()).isEqualTo("USER");
    assertThat(response.getStatus()).isEqualTo("ACTIVE");
    assertThat(response.getNotificationConsentYn()).isTrue();
    assertThat(response.getMydataConsentYn()).isFalse();
    assertThat(response.getFreelancerYn()).isTrue();
    assertThat(response.getJobType()).isEqualTo("DEVELOPER");
    assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 0, 0));
    System.out.println("        ✓ userId       = 1");
    System.out.println("        ✓ email        = \"user@test.com\"");
    System.out.println("        ✓ userName     = \"홍길동\"");
    System.out.println("        ✓ phoneNumber  = \"01012345678\"");
    System.out.println("        ✓ role         = \"USER\"");
    System.out.println("        ✓ status       = \"ACTIVE\"");
    System.out.println("        ✓ notificationConsentYn = true");
    System.out.println("        ✓ mydataConsentYn       = false");
    System.out.println("        ✓ freelancerYn = true  (UserProfile에서)");
    System.out.println("        ✓ jobType      = \"DEVELOPER\"  (UserProfile에서)");
    System.out.println("        ✓ createdAt    = 2026-05-01T10:00:00");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // I-02. User 없음
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("I-02: User 없음 - USER_001 예외, userProfileRepository 조회 미호출")
  void getMe_userNotFound() {
    System.out.println("\n=== I-02: User 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = Optional.empty()");
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] userService.getMe(1L) 호출");
    System.out.println("[Then 1] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> userService.getMe(1L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
    System.out.println("        ✓ BusinessException(USER_001) 발생 (사용자를 찾을 수 없음)");

    System.out.println("[Then 2] userProfileRepository 조회 미호출 확인");
    then(userProfileRepository).shouldHaveNoInteractions();
    System.out.println("        ✓ userProfileRepository 호출되지 않음 (User 조회 실패 시 즉시 종료)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // I-03. UserProfile 없음
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("I-03: UserProfile 없음 - USER_001 예외 발생")
  void getMe_profileNotFound() {
    System.out.println("\n=== I-03: UserProfile 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = User 존재");
    given(userRepository.findById(1L)).willReturn(Optional.of(buildUser()));

    System.out.println("[Given] userProfileRepository.findByUserId(1L) = Optional.empty()");
    given(userProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] userService.getMe(1L) 호출");
    System.out.println("[Then] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> userService.getMe(1L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
    System.out.println("       ✓ BusinessException(USER_001) 발생 (프로필을 찾을 수 없음)");

    System.out.println("\n=== PASSED ===\n");
  }
}
