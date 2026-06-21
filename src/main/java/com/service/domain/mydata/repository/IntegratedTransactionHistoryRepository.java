package com.service.domain.mydata.repository;

import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegratedTransactionHistoryRepository
    extends JpaRepository<IntegratedTransactionHistory, Long> {

  /** 마지막 동기화 ID 이후 신규 거래내역 조회 (분석 DB 폴링용, 배치 크기 제한) */
  List<IntegratedTransactionHistory>
      findByIntegratedTransactionIdGreaterThanOrderByIntegratedTransactionIdAsc(
          Long integratedTransactionId, Pageable pageable);

  /** 사용자별 특정 기관의 최신 거래내역 1건 조회 (자산 스냅샷용) */
  Optional<IntegratedTransactionHistory>
      findFirstByUserIdAndInstitutionTypeOrderByTransactionOccurredAtDesc(
          Long userId, String institutionType);

  /** 계좌별 마지막 저장 거래내역 1건 조회 (증분 동기화 기준점) */
  Optional<IntegratedTransactionHistory>
      findFirstByLinkedAccountIdOrderByTransactionOccurredAtDesc(Long linkedAccountId);

  /** 원본 거래 ID 중복 체크 */
  boolean existsByOriginalTransactionId(Long originalTransactionId);

  /** 계좌별 기간 거래내역 조회 (최신순) */
  List<IntegratedTransactionHistory>
      findByLinkedAccountIdAndTransactionOccurredAtBetweenOrderByTransactionOccurredAtDesc(
          Long linkedAccountId, LocalDateTime from, LocalDateTime to);

  /** 거래내역이 존재하는 사용자 ID 목록 조회 */
  @Query("SELECT DISTINCT h.userId FROM IntegratedTransactionHistory h")
  List<Long> findAllDistinctUserIds();

  /** 여러 사용자의 기관별 최신 거래내역을 한 번에 조회 (N+1 방지) */
  @Query(
      value =
          "SELECT DISTINCT ON (h.user_id) h.* "
              + "FROM integrated_transaction_history h "
              + "WHERE h.institution_type = :institutionType AND h.user_id IN :userIds "
              + "ORDER BY h.user_id, h.transaction_occurred_at DESC",
      nativeQuery = true)
  List<IntegratedTransactionHistory> findLatestByUserIdsAndInstitutionType(
      @Param("userIds") List<Long> userIds, @Param("institutionType") String institutionType);
}
