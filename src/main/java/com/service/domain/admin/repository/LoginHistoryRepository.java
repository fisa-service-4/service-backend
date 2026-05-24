package com.service.domain.admin.repository;

import com.service.domain.admin.entity.LoginHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoginHistoryRepository
    extends JpaRepository<LoginHistory, Long>, JpaSpecificationExecutor<LoginHistory> {

  Optional<LoginHistory> findTopByUserIdAndLoginTypeOrderByLoggedAtDesc(
      Long userId, String loginType);
}
