package com.service.domain.mydata.repository;

import com.service.domain.mydata.entity.AccountMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMappingRepository extends JpaRepository<AccountMapping, Long> {

  Optional<AccountMapping> findByUserIdAndMappingType(
      Long userId, AccountMapping.MappingType mappingType);
}
