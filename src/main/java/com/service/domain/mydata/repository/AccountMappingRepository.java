package com.service.domain.mydata.repository;

import com.service.domain.mydata.entity.AccountMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMappingRepository extends JpaRepository<AccountMapping, Long> {

  Optional<AccountMapping> findByUserIdAndMappingType(
      Long userId, AccountMapping.MappingType mappingType);

  List<AccountMapping> findByUserId(Long userId);

  Optional<AccountMapping> findByLinkedFinancialAccount_LinkedAccountId(Long linkedAccountId);
}
