package com.service.domain.user.repository;

import com.service.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  Optional<User> findByEmail(String email);

  Optional<User> findByPhoneNumber(String phoneNumber);

  boolean existsByEmail(String email);

  boolean existsByPhoneNumber(String phoneNumber);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.userId = :userId")
  Optional<User> findByIdWithProfile(@Param("userId") Long userId);

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
