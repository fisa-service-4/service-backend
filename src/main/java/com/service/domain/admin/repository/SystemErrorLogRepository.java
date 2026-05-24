package com.service.domain.admin.repository;

import com.service.domain.admin.entity.SystemErrorLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemErrorLogRepository
    extends JpaRepository<SystemErrorLog, Long>, JpaSpecificationExecutor<SystemErrorLog> {

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
