package com.service.domain.virtualsalary.repository;

import com.service.domain.virtualsalary.entity.DistributionHistory;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DistributionHistoryRepository extends JpaRepository<DistributionHistory, Long> {

  @Query(
      "SELECT COALESCE(SUM(d.emergencyAmount), 0) FROM DistributionHistory d WHERE d.userId = :userId")
  BigDecimal sumEmergencyAmountByUserId(@Param("userId") Long userId);
}
