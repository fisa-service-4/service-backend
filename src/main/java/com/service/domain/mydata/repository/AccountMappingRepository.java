package com.service.domain.mydata.repository;

import com.service.domain.mydata.entity.AccountMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountMappingRepository extends JpaRepository<AccountMapping, Long> {

  Optional<AccountMapping> findByUserIdAndMappingType(
      Long userId, AccountMapping.MappingType mappingType);

  List<AccountMapping> findByUserId(Long userId);

  Optional<AccountMapping> findByLinkedFinancialAccount_LinkedAccountId(Long linkedAccountId);

  @Query(
      "SELECT am FROM AccountMapping am JOIN FETCH am.linkedFinancialAccount"
          + " WHERE am.userId = :userId AND am.mappingType = :mappingType")
  Optional<AccountMapping> findByUserIdAndMappingTypeFetch(
      @Param("userId") Long userId,
      @Param("mappingType") AccountMapping.MappingType mappingType);
}
