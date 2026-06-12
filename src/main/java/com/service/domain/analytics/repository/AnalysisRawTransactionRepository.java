package com.service.domain.analytics.repository;

import com.service.domain.analytics.entity.AnalysisRawTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnalysisRawTransactionRepository
    extends JpaRepository<AnalysisRawTransaction, Long> {

  /** 마지막으로 동기화된 source_transaction_id (= integrated_transaction_id) 조회 */
  @Query("SELECT MAX(r.sourceTransactionId) FROM AnalysisRawTransaction r")
  Optional<Long> findMaxSourceTransactionId();

  /** 분석 데이터가 존재하는 전체 사용자 ID 목록 조회 */
  @Query("SELECT DISTINCT r.userId FROM AnalysisRawTransaction r")
  List<Long> findDistinctUserIds();
}
