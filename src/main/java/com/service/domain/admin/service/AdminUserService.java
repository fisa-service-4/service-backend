package com.service.domain.admin.service;

import com.service.domain.admin.dto.request.UserStatusUpdateRequest;
import com.service.domain.admin.dto.response.AdminUserDetailResponse;
import com.service.domain.admin.dto.response.AdminUserListResponse;
import com.service.domain.admin.entity.LoginHistory;
import com.service.domain.admin.repository.LoginHistoryRepository;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import com.service.domain.user.repository.UserRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

  private static final String REFRESH_TOKEN_PATTERN = "refresh:*";

  private final UserRepository userRepository;
  private final LoginHistoryRepository loginHistoryRepository;
  private final PinAuthRepository pinAuthRepository;
  private final StringRedisTemplate redisTemplate;

  private static final Map<String, String> SORT_FIELD_MAP =
      Map.of("name", "userName", "createdAt", "createdAt", "email", "email", "status", "status");

  @Transactional(readOnly = true)
  public Page<AdminUserListResponse> getUsers(
      String keyword,
      User.Status status,
      String jobType,
      String loginStatus,
      String sort,
      Pageable pageable) {
    Set<Long> onlineUserIds = getOnlineUserIds();

    Pageable effectivePageable;
    if ("name".equals(sort)) {
      effectivePageable =
          PageRequest.of(
              pageable.getPageNumber(), pageable.getPageSize(), Sort.by("userName").ascending());
    } else {
      effectivePageable = translateSort(pageable);
    }

    Specification<User> spec = buildSpec(keyword, status, jobType, loginStatus, onlineUserIds);
    return userRepository
        .findAll(spec, effectivePageable)
        .map(
            user -> AdminUserListResponse.of(user, null, onlineUserIds.contains(user.getUserId())));
  }

  private Pageable translateSort(Pageable pageable) {
    List<Sort.Order> orders =
        StreamSupport.stream(pageable.getSort().spliterator(), false)
            .map(
                order -> {
                  String mapped =
                      SORT_FIELD_MAP.getOrDefault(order.getProperty(), order.getProperty());
                  return new Sort.Order(order.getDirection(), mapped);
                })
            .collect(Collectors.toList());
    if (orders.isEmpty()) {
      return pageable;
    }
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
  }

  @Transactional(readOnly = true)
  public AdminUserDetailResponse getUserDetail(Long userId) {
    User user =
        userRepository
            .findByIdWithProfile(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

    LocalDateTime lastLoginAt =
        loginHistoryRepository
            .findTopByUserIdAndLoginTypeOrderByLoggedAtDesc(userId, "LOGIN")
            .map(LoginHistory::getLoggedAt)
            .orElse(null);

    boolean isOnline = Boolean.TRUE.equals(redisTemplate.hasKey("refresh:" + userId));

    return AdminUserDetailResponse.of(user, lastLoginAt, isOnline);
  }

  @Transactional
  public void updateUserStatus(Long userId, UserStatusUpdateRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    if (User.Status.ACTIVE.equals(request.getStatus())) {
      pinAuthRepository.findByUserId(userId).ifPresent(pinAuth -> pinAuth.unlock());
    }
    user.updateStatus(request.getStatus());
  }

  private Specification<User> buildSpec(
      String keyword,
      User.Status status,
      String jobType,
      String loginStatus,
      Set<Long> onlineUserIds) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(cb.equal(root.get("role"), User.Role.USER));

      if (keyword != null && !keyword.isBlank()) {
        String like = "%" + keyword.toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("userName")), like),
                cb.like(cb.lower(root.get("email")), like)));
      }

      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }

      if (jobType != null && !jobType.isBlank()) {
        Join<User, UserProfile> profileJoin = root.join("profile", JoinType.LEFT);
        predicates.add(cb.equal(profileJoin.get("jobType"), jobType));
      }

      if ("ONLINE".equals(loginStatus)) {
        if (onlineUserIds.isEmpty()) {
          predicates.add(cb.disjunction());
        } else {
          predicates.add(root.get("userId").in(onlineUserIds));
        }
      } else if ("OFFLINE".equals(loginStatus)) {
        if (!onlineUserIds.isEmpty()) {
          predicates.add(cb.not(root.get("userId").in(onlineUserIds)));
        }
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private Set<Long> getOnlineUserIds() {
    Set<Long> onlineIds = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions().match(REFRESH_TOKEN_PATTERN).count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext()) {
        String key = cursor.next();
        try {
          onlineIds.add(Long.parseLong(key.replace("refresh:", "")));
        } catch (NumberFormatException ignored) {
          // skip malformed keys
        }
      }
    }
    return onlineIds;
  }
}
