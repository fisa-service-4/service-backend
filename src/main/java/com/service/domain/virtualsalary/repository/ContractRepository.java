package com.service.domain.virtualsalary.repository;

import com.service.domain.virtualsalary.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {}
