package com.service.domain.virtualsalary.repository;

import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMatchingRepository extends JpaRepository<PaymentMatching, Long> {

  @Query(
      "SELECT pm FROM PaymentMatching pm"
          + " WHERE pm.contract.userId = :userId"
          + " AND (:contractId IS NULL OR pm.contract.contractId = :contractId)"
          + " AND (:matchingStatus IS NULL OR pm.matchingStatus = :matchingStatus)"
          + " AND (:from IS NULL OR pm.matchedAt >= :from)"
          + " AND (:to IS NULL OR pm.matchedAt <= :to)")
  List<PaymentMatching> findAllByFilters(
      @Param("userId") Long userId,
      @Param("contractId") Long contractId,
      @Param("matchingStatus") MatchingStatus matchingStatus,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

  Optional<PaymentMatching> findByMatchingIdAndContract_UserId(Long matchingId, Long userId);
}
