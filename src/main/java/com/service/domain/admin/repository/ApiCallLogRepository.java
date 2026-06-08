package com.service.domain.admin.repository;

import com.service.domain.admin.entity.ApiCallLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiCallLogRepository
    extends JpaRepository<ApiCallLog, Long>, JpaSpecificationExecutor<ApiCallLog> {

  long countByRequestedAtBetween(LocalDateTime start, LocalDateTime end);

  @Query(
      "SELECT AVG(a.durationMs) FROM ApiCallLog a "
          + "WHERE a.requestedAt BETWEEN :start AND :end AND a.durationMs IS NOT NULL")
  Double avgDurationMsByRequestedAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
