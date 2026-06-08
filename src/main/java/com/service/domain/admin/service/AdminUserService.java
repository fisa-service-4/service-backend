package com.service.domain.admin.service;

import com.service.domain.admin.dto.request.UserStatusUpdateRequest;
import com.service.domain.admin.dto.response.AdminUserDetailResponse;
import com.service.domain.admin.dto.response.AdminUserListResponse;
import com.service.domain.admin.entity.LoginHistory;
import com.service.domain.admin.repository.LoginHistoryRepository;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

  private final UserRepository userRepository;
  private final LoginHistoryRepository loginHistoryRepository;

  private static final Map<String, String> SORT_FIELD_MAP =
      Map.of("name", "userName", "createdAt", "createdAt", "email", "email", "status", "status");

  @Transactional(readOnly = true)
  public Page<AdminUserListResponse> getUsers(
      String keyword, User.Status status, String jobType, Pageable pageable) {
    Specification<User> spec = buildSpec(keyword, status, jobType);
    return userRepository
        .findAll(spec, translateSort(pageable))
        .map(user -> AdminUserListResponse.of(user, null));
  }

  private Pageable translateSort(Pageable pageable) {
    List<Sort.Order> orders =
        StreamSupport.stream(pageable.getSort().spliterator(), false)
            .map(
                order -> {
                  String mapped = SORT_FIELD_MAP.getOrDefault(order.getProperty(), order.getProperty());
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

    return AdminUserDetailResponse.of(user, lastLoginAt);
  }

  @Transactional
  public void updateUserStatus(Long userId, UserStatusUpdateRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    user.updateStatus(request.getStatus());
  }

  private Specification<User> buildSpec(String keyword, User.Status status, String jobType) {
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

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
