package com.service.domain.admin.repository;

import com.service.domain.admin.entity.ApiCallLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApiCallLogRepository
    extends JpaRepository<ApiCallLog, Long>, JpaSpecificationExecutor<ApiCallLog> {

  long countByRequestedAtBetween(LocalDateTime start, LocalDateTime end);
}
