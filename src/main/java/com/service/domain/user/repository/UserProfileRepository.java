package com.service.domain.user.repository;

import com.service.domain.user.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  // 사용자 ID로 프로필 조회
  // 내 정보 조회 API (GET /users/me)
  // 프로필 수정 API (PATCH /users/me)
  Optional<UserProfile> findByUserId(Long userId);
}
