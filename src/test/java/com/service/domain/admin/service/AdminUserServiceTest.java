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
    System.out.println("\n=== K-01: getUserDetail isOnline=true ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findByIdWithProfile(1L) = User 존재");
    User user = buildUser();
    given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.of(user));

    System.out.println("[Given] loginHistoryRepository.findTop... = Optional.empty()");
    given(loginHistoryRepository.findTopByUserIdAndLoginTypeOrderByLoggedAtDesc(1L, "LOGIN"))
        .willReturn(Optional.empty());

    System.out.println("[Given] redisTemplate.hasKey(\"refresh:1\") = true");
    given(redisTemplate.hasKey("refresh:1")).willReturn(true);

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] adminUserService.getUserDetail(1L) 호출");
    AdminUserDetailResponse response = adminUserService.getUserDetail(1L);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] response.getIsOnline() = true 확인");
    assertThat(response.getIsOnline()).isTrue();
    System.out.println("        ✓ isOnline = true (Redis \"refresh:1\" 키 존재)");

    assertThat(response.getUserId()).isEqualTo(1L);
    assertThat(response.getStatus()).isEqualTo("ACTIVE");
    System.out.println("        ✓ userId=1, status=ACTIVE");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-02. getUserDetail — isOnline=false
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-02: getUserDetail — Redis refresh:1 키 없음 → isOnline=false")
  void getUserDetail_isOnlineFalse() {
    System.out.println("\n=== K-02: getUserDetail isOnline=false ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findByIdWithProfile(1L) = User 존재");
    given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.of(buildUser()));

    given(loginHistoryRepository.findTopByUserIdAndLoginTypeOrderByLoggedAtDesc(1L, "LOGIN"))
        .willReturn(Optional.empty());

    System.out.println("[Given] redisTemplate.hasKey(\"refresh:1\") = false");
    given(redisTemplate.hasKey("refresh:1")).willReturn(false);

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] adminUserService.getUserDetail(1L) 호출");
    AdminUserDetailResponse response = adminUserService.getUserDetail(1L);

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] response.getIsOnline() = false 확인");
    assertThat(response.getIsOnline()).isFalse();
    System.out.println("        ✓ isOnline = false (Redis \"refresh:1\" 키 없음)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-03. getUserDetail — User 없음 → USER_001
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-03: getUserDetail — User 없음 → USER_001 예외, loginHistory·Redis 미호출")
  void getUserDetail_userNotFound() {
    System.out.println("\n=== K-03: getUserDetail User 없음 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] userRepository.findByIdWithProfile(1L) = Optional.empty()");
    given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.empty());

    // ── When & Then ────────────────────────────────────────────
    System.out.println("\n[When] adminUserService.getUserDetail(1L) 호출");
    System.out.println("[Then] BusinessException(USER_001) 발생 확인");
    assertThatThrownBy(() -> adminUserService.getUserDetail(1L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
    System.out.println("        ✓ BusinessException(USER_001) 발생 (사용자를 찾을 수 없음)");

    System.out.println("[Then] loginHistoryRepository, redisTemplate 미호출 확인");
    then(loginHistoryRepository).shouldHaveNoInteractions();
    then(redisTemplate).shouldHaveNoInteractions();
    System.out.println("        ✓ 이후 로직 실행 안 됨 (User 조회 실패 시 즉시 예외)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-04. getUsers — translateSort 알 수 없는 필드 → 그대로 통과
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-04: getUsers — translateSort 알 수 없는 sort 필드 → 그대로 통과")
  @SuppressWarnings("unchecked")
  void getUsers_translateSort_unknownFieldPassThrough() {
    System.out.println("\n=== K-04: translateSort 알 수 없는 필드 통과 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] Redis scan → 빈 커서 (onlineUserIds = empty)");
    given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
    given(cursor.hasNext()).willReturn(false);

    System.out.println("[Given] userRepository.findAll(spec, pageable) → Page.empty()");
    given(userRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "unknownField"));
    System.out.println("[Given] Pageable sort='unknownField' DESC — SORT_FIELD_MAP에 없는 필드");

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] adminUserService.getUsers(sort=null, pageable) 호출");
    adminUserService.getUsers(null, null, null, null, null, pageable);

    // ── Then ───────────────────────────────────────────────────
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    then(userRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
    Pageable captured = pageableCaptor.getValue();

    System.out.println("\n[Then] findAll에 전달된 sort 필드 확인");
    List<Sort.Order> orders = captured.getSort().toList();
    assertThat(orders).hasSize(1);
    assertThat(orders.get(0).getProperty()).isEqualTo("unknownField");
    System.out.println("        ✓ sort='unknownField' 그대로 통과 (SORT_FIELD_MAP.getOrDefault → 원본 반환)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // K-05. getUsers — translateSort 정렬 없음 → 원본 pageable 반환
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("K-05: getUsers — sort 없는 Pageable → translateSort가 원본 pageable 그대로 반환")
  @SuppressWarnings("unchecked")
  void getUsers_translateSort_noSortOrdersReturnOriginal() {
    System.out.println("\n=== K-05: translateSort 정렬 없음 → 원본 pageable 반환 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] Redis scan → 빈 커서");
    given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
    given(cursor.hasNext()).willReturn(false);

    given(userRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    Pageable pageable = PageRequest.of(2, 5); // sort 없음
    System.out.println("[Given] Pageable(page=2, size=5) — sort 없음");

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] adminUserService.getUsers(sort=null, pageable) 호출");
    adminUserService.getUsers(null, null, null, null, null, pageable);

    // ── Then ───────────────────────────────────────────────────
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    then(userRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
    Pageable captured = pageableCaptor.getValue();

    System.out.println("\n[Then] 원본 pageable 그대로 반환 확인");
    assertThat(captured.getPageNumber()).isEqualTo(2);
    assertThat(captured.getPageSize()).isEqualTo(5);
    assertThat(captured.getSort().isSorted()).isFalse();
    System.out.println("        ✓ page=2, size=5, sort=없음");
    System.out.println("        ✓ orders.isEmpty() → translateSort가 원본 pageable 반환");

    System.out.println("\n=== PASSED ===\n");
  }
}
