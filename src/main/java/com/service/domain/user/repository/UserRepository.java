package com.service.domain.user.repository;

import com.service.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByPhoneNumber(String phoneNumber);

  boolean existsByEmail(String email);

  boolean existsByPhoneNumber(String phoneNumber);
}
