package com.service.domain.virtualsalary.repository;

import com.service.domain.virtualsalary.entity.Contract;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

  List<Contract> findAllByUserIdAndExpectedPaymentDateBetween(
      Long userId, LocalDate startDate, LocalDate endDate);

  Optional<Contract> findByContractIdAndUserId(Long contractId, Long userId);
}
