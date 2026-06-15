package com.service.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.admin.dto.response.AdminUserDetailResponse;
import com.service.domain.admin.repository.LoginHistoryRepository;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private LoginHistoryRepository loginHistoryRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private Cursor<String> cursor;

  @InjectMocks private AdminUserService adminUserService;

  private User buildUser() {
    return User.builder()
        .userId(1L)
        .firebaseUid("firebase-uid")
        .email("user@test.com")
        .passwordHash("hashed")
        .userName("홍길동")
        .phoneNumber("01012345678")
        .role(User.Role.USER)
        .status(User.Status.ACTIVE)
        .notificationConsentYn(true)
        .termsConsentYn(true)
        .mydataConsentYn(false)
        .build();
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-01. getUserDetail — isOnline=true
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-01: getUserDetail — Redis refresh:1 키 존재 → isOnline=true")
  void getUserDetail_isOnlineTrue() {
    User user = buildUser();
    given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.of(user));
    given(loginHistoryRepository.findTopByUserIdAndLoginTypeOrderByLoggedAtDesc(1L, "LOGIN"))
        .willReturn(Optional.empty());
    given(redisTemplate.hasKey("refresh:1")).willReturn(true);

    AdminUserDetailResponse response = adminUserService.getUserDetail(1L);

    assertThat(response.getIsOnline()).isTrue();
    assertThat(response.getUserId()).isEqualTo(1L);
    assertThat(response.getStatus()).isEqualTo("ACTIVE");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-02. getUserDetail — isOnline=false
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-02: getUserDetail — Redis refresh:1 키 없음 → isOnline=false")
  void getUserDetail_isOnlineFalse() {
    given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.of(buildUser()));
    given(loginHistoryRepository.findTopByUserIdAndLoginTypeOrderByLoggedAtDesc(1L, "LOGIN"))
        .willReturn(Optional.empty());
    given(redisTemplate.hasKey("refresh:1")).willReturn(false);

    AdminUserDetailResponse response = adminUserService.getUserDetail(1L);

    assertThat(response.getIsOnline()).isFalse();
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-03. getUserDetail — User 없음 → USER_001
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-03: getUserDetail — User 없음 → USER_001 예외, loginHistory·Redis 미호출")
  void getUserDetail_userNotFound() {
    given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> adminUserService.getUserDetail(1L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);

    then(loginHistoryRepository).shouldHaveNoInteractions();
    then(redisTemplate).shouldHaveNoInteractions();
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-04. getUsers — translateSort 알 수 없는 필드 → 그대로 통과
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-04: getUsers — translateSort 알 수 없는 sort 필드 → 그대로 통과")
  @SuppressWarnings("unchecked")
  void getUsers_translateSort_unknownFieldPassThrough() {
    given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
    given(cursor.hasNext()).willReturn(false);
    given(userRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "unknownField"));
    adminUserService.getUsers(null, null, null, null, null, pageable);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    then(userRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
    Pageable captured = pageableCaptor.getValue();

    List<Sort.Order> orders = captured.getSort().toList();
    assertThat(orders).hasSize(1);
    assertThat(orders.get(0).getProperty()).isEqualTo("unknownField");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-05. getUsers — translateSort 정렬 없음 → 원본 pageable 반환
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-05: getUsers — sort 없는 Pageable → translateSort가 원본 pageable 그대로 반환")
  @SuppressWarnings("unchecked")
  void getUsers_translateSort_noSortOrdersReturnOriginal() {
    given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
    given(cursor.hasNext()).willReturn(false);
    given(userRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    Pageable pageable = PageRequest.of(2, 5);
    adminUserService.getUsers(null, null, null, null, null, pageable);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    then(userRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
    Pageable captured = pageableCaptor.getValue();

    assertThat(captured.getPageNumber()).isEqualTo(2);
    assertThat(captured.getPageSize()).isEqualTo(5);
    assertThat(captured.getSort().isSorted()).isFalse();
  }
}
