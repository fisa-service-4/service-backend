package com.service.domain.virtualsalary.repository;

import com.service.domain.virtualsalary.entity.PaymentMatching;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMatchingRepository extends JpaRepository<PaymentMatching, Long> {

  @Query("SELECT pm FROM PaymentMatching pm WHERE pm.contract.userId = :userId")
  List<PaymentMatching> findAllByFilters(@Param("userId") Long userId);

  Optional<PaymentMatching> findByMatchingIdAndContract_UserId(Long matchingId, Long userId);
}
