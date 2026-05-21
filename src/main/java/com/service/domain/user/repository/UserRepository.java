package com.service.domain.user.repository;

import com.service.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  // 로그인 API (POST /auth/login) - 이메일로 사용자 조회
  // 회원가입 API (POST /auth/signup) - 이메일 중복 검사 시 조회 후 예외 처리
  Optional<User> findByEmail(String email);

  // 휴대폰 인증 요청 API (POST /auth/phone/send) - 전화번호로 사용자 조회
  Optional<User> findByPhoneNumber(String phoneNumber);

  // 회원가입 API (POST /auth/signup) - 이메일 중복 검사 (AUTH_001)
  boolean existsByEmail(String email);

  // 휴대폰 인증 요청 API (POST /auth/phone/send) - 전화번호 중복 검사 (AUTH_002)
  boolean existsByPhoneNumber(String phoneNumber);
}
