package com.service.domain.admin.repository;

import com.service.domain.admin.entity.SystemErrorLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemErrorLogRepository
    extends JpaRepository<SystemErrorLog, Long>, JpaSpecificationExecutor<SystemErrorLog> {

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  @Modifying
  @Query(
      "UPDATE SystemErrorLog s SET s.resolvedYn = true, s.resolvedAt = :now"
          + " WHERE s.userId = :userId AND s.errorCode = :errorCode AND s.resolvedYn = false")
  int resolveAllByUserIdAndErrorCode(
      @Param("userId") Long userId,
      @Param("errorCode") String errorCode,
      @Param("now") LocalDateTime now);
}
