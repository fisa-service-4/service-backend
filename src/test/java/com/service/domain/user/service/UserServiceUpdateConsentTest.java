package com.service.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.user.dto.request.ConsentUpdateRequest;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateConsentTest {

  @Mock private UserRepository userRepository;
  @Mock private UserProfileRepository userProfileRepository;

  @InjectMocks private UserService userService;

  private ConsentUpdateRequest consentRequest(boolean notificationConsentYn) {
    ConsentUpdateRequest request = new ConsentUpdateRequest();
    ReflectionTestUtils.setField(request, "notificationConsentYn", notificationConsentYn);
    return request;
  }

  private User buildUser(boolean notificationConsentYn) {
    return User.builder()
        .userId(1L)
        .firebaseUid("firebase-uid")
        .email("user@test.com")
        .passwordHash("hashed-password")
        .userName("홍길동")
        .phoneNumber("01012345678")
        .role(User.Role.USER)
        .status(User.Status.ACTIVE)
        .notificationConsentYn(notificationConsentYn)
        .termsConsentYn(true)
        .mydataConsentYn(false)
        .build();
  }

  // ────────────────────────────────────────────────────────────────────────────
  // J-01. 알림 동의 ON
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("J-01: 알림 동의 ON - notificationConsentYn false → true 변경 확인")
  void updateConsent_turnOn() {
    System.out.println("\n=== J-01: 알림 동의 ON ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] User(notificationConsentYn=false) — 알림 미동의 상태");
    User user = buildUser(false);
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    System.out.println("[Given] request.getNotificationConsentYn() = true");
    ConsentUpdateRequest request = consentRequest(true);

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] userService.updateConsent(1L, request) 호출");
    userService.updateConsent(1L, request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] user.updateNotificationConsent(true) 호출 확인");
    assertThat(user.getNotificationConsentYn()).isTrue();
    System.out.println("        ✓ notificationConsentYn = true (false → true 변경됨)");

    System.out.println("[Then] userRepository.save() 미호출 확인 (dirty checking으로 반영)");
    then(userRepository).should().findById(1L);
    then(userRepository).shouldHaveNoMoreInteractions();
    System.out.println("        ✓ save() 호출되지 않음 (@Transactional dirty checking으로 자동 반영)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // J-02. 알림 동의 OFF
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("J-02: 알림 동의 OFF - notificationConsentYn true → false 변경 확인")
  void updateConsent_turnOff() {
    System.out.println("\n=== J-02: 알림 동의 OFF ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] User(notificationConsentYn=true) — 알림 동의 상태");
    User user = buildUser(true);
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    System.out.println("[Given] request.getNotificationConsentYn() = false");
    ConsentUpdateRequest request = consentRequest(false);

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] userService.updateConsent(1L, request) 호출");
    userService.updateConsent(1L, request);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] user.updateNotificationConsent(false) 호출 확인");
    assertThat(user.getNotificationConsentYn()).isFalse();
    System.out.println("        ✓ notificationConsentYn = false (true → false 변경됨)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // J-03. User 없음
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("J-03: User 없음 - USER_001 예외 발생")
  void updateConsent_userNotFound() {
    System.out.println("\n=== J-03: User 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findById(1L) = Optional.empty()");
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] userService.updateConsent(1L, request) 호출");
    System.out.println("[Then] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> userService.updateConsent(1L, consentRequest(true)))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
    System.out.println("       ✓ BusinessException(USER_001) 발생 (사용자를 찾을 수 없음)");

    System.out.println("\n=== PASSED ===\n");
  }
}
