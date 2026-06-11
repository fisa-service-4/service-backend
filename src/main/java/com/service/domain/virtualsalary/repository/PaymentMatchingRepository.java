package com.service.domain.virtualsalary.repository;

import com.service.domain.virtualsalary.entity.PaymentMatching;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMatchingRepository extends JpaRepository<PaymentMatching, Long> {

  @Query("SELECT pm FROM PaymentMatching pm WHERE pm.contract.userId = :userId")
  List<PaymentMatching> findAllByFilters(@Param("userId") Long userId);

  Optional<PaymentMatching> findByMatchingIdAndContract_UserId(Long matchingId, Long userId);

  @Query(
      "SELECT pm FROM PaymentMatching pm"
          + " WHERE pm.matchingStatus = 'TBC'"
          + " AND pm.contract.userId = :userId"
          + " ORDER BY pm.contract.expectedPaymentDate ASC")
  List<PaymentMatching> findTbcByUserId(@Param("userId") Long userId);

  @Query(
      "SELECT pm FROM PaymentMatching pm"
          + " WHERE pm.matchingStatus = 'TBC'"
          + " AND pm.contract.expectedPaymentDate < :cutoffDate")
  List<PaymentMatching> findExpiredTbc(@Param("cutoffDate") LocalDate cutoffDate);

  @Query(
      "SELECT DISTINCT pm.contract.userId FROM PaymentMatching pm WHERE pm.matchingStatus = 'TBC'")
  List<Long> findDistinctUserIdsWithTbcMatchings();

  @Query(
      "SELECT pm.bankTransactionId FROM PaymentMatching pm"
          + " WHERE pm.contract.userId = :userId"
          + " AND pm.matchingStatus = 'MATCHED'"
          + " AND pm.bankTransactionId IS NOT NULL")
  Set<Long> findMatchedTransactionIdsByUserId(@Param("userId") Long userId);

  @Query(
      "SELECT pm FROM PaymentMatching pm"
          + " WHERE pm.matchingStatus = 'MATCHED'"
          + " AND pm.matchedBy = 'SYSTEM'"
          + " AND pm.distributedYn = false")
  List<PaymentMatching> findMatchedWithoutDistribution();

  @Query(
      "SELECT pm FROM PaymentMatching pm"
          + " WHERE pm.matchingStatus = 'MATCHED'"
          + " AND pm.matchedBy = 'SYSTEM'"
          + " AND pm.distributedYn = false"
          + " AND pm.contract.userId = :userId")
  List<PaymentMatching> findMatchedWithoutDistributionByUserId(@Param("userId") Long userId);
}
