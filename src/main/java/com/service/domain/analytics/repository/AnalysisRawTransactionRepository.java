package com.service.domain.analytics.repository;

import com.service.domain.analytics.entity.AnalysisRawTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnalysisRawTransactionRepository
    extends JpaRepository<AnalysisRawTransaction, Long> {

  /** 마지막으로 동기화된 source_transaction_id (= integrated_transaction_id) 조회 */
  @Query("SELECT MAX(r.sourceTransactionId) FROM AnalysisRawTransaction r")
  Optional<Long> findMaxSourceTransactionId();
}
