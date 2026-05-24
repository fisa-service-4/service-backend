package com.service.domain.admin.repository;

import com.service.domain.admin.entity.AiUsageLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AiUsageLogRepository
    extends JpaRepository<AiUsageLog, Long>, JpaSpecificationExecutor<AiUsageLog> {

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
