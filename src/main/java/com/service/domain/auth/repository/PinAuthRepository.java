package com.service.domain.auth.repository;

import com.service.domain.auth.entity.PinAuth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinAuthRepository extends JpaRepository<PinAuth, Long> {

  // 사용자 ID로 PIN 정보 조회
  // PIN 등록 API (POST /auth/pin)
  // PIN 검증 API (POST /auth/pin/verify)
  // PIN 변경 API (PATCH /auth/pin)
  Optional<PinAuth> findByUserId(Long userId);

  // PIN 등록 여부 확인
  // PIN 등록 API (POST /auth/pin) - 이미 등록된 PIN이 있는지 확인
  boolean existsByUserId(Long userId);

  long countByLockedYnTrue();
}
