package com.service.domain.mydata.repository;

import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IntegratedTransactionHistoryRepository
    extends JpaRepository<IntegratedTransactionHistory, Long> {

  /** 마지막 동기화 ID 이후 신규 거래내역 조회 (분석 DB 폴링용) */
  List<IntegratedTransactionHistory>
      findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
          Long integratedTransactionId);

  /** 사용자별 특정 기관의 최신 거래내역 1건 조회 (자산 스냅샷용) */
  Optional<IntegratedTransactionHistory>
      findFirstByUserIdAndInstitutionTypeOrderByTransactionOccurredAtDesc(
          Long userId, String institutionType);

  /** 거래내역이 존재하는 사용자 ID 목록 조회 */
  @Query("SELECT DISTINCT h.userId FROM IntegratedTransactionHistory h")
  List<Long> findAllDistinctUserIds();
}
