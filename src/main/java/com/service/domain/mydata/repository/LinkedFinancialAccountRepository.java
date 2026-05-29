package com.service.domain.mydata.repository;

import com.service.domain.mydata.entity.LinkedFinancialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkedFinancialAccountRepository
    extends JpaRepository<LinkedFinancialAccount, Long> {

  Optional<LinkedFinancialAccount> findByExternalAccountIdAndUser_UserId(
      Long externalAccountId, Long userId);

  boolean existsByExternalAccountId(Long externalAccountId);
}
