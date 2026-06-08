package com.service.domain.mydata.repository;

import com.service.domain.mydata.entity.IntegratedStockTransactionHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegratedStockTransactionHistoryRepository
    extends JpaRepository<IntegratedStockTransactionHistory, Long> {

  /** 사용자의 가장 최근 주식 거래내역 1건 조회 (예수금 기반 stock asset 추정용) */
  Optional<IntegratedStockTransactionHistory> findFirstByUserIdOrderByTransactionOccurredAtDesc(
      Long userId);

  /** 주식 거래내역이 존재하는 사용자 ID 목록 조회 */
  @Query("SELECT DISTINCT h.userId FROM IntegratedStockTransactionHistory h")
  List<Long> findAllDistinctUserIds();

  /** 여러 사용자의 최신 주식 거래내역을 한 번에 조회 (N+1 방지) */
  @Query(
      value =
          "SELECT DISTINCT ON (h.user_id) h.* "
              + "FROM integrated_stock_transaction_history h "
              + "WHERE h.user_id IN :userIds "
              + "ORDER BY h.user_id, h.transaction_occurred_at DESC",
      nativeQuery = true)
  List<IntegratedStockTransactionHistory> findLatestByUserIds(@Param("userIds") List<Long> userIds);
}
