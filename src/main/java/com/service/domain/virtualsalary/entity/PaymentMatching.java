package com.service.domain.virtualsalary.entity;

import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "payment_matching")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentMatching {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "matching_id")
  private Long matchingId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  @Column(name = "bank_transaction_id")
  private Long bankTransactionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "matching_status", nullable = false)
  private MatchingStatus matchingStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "matched_by", nullable = false)
  private MatchedBy matchedBy;

  @Column(name = "matched_at")
  private LocalDateTime matchedAt;

  @Column(name = "transaction_amount", precision = 18, scale = 2)
  private BigDecimal transactionAmount;

  @Builder.Default
  @Column(name = "distributed_yn", nullable = false)
  private boolean distributedYn = false;

  public void markDistributed() {
    this.distributedYn = true;
  }

  public void complete(Long bankTransactionId) {
    this.bankTransactionId = bankTransactionId;
    this.matchingStatus = MatchingStatus.MATCHED;
    this.matchedBy = MatchedBy.USER;
    this.matchedAt = LocalDateTime.now();
  }

  public void autoMatch(Long bankTransactionId, BigDecimal transactionAmount) {
    this.bankTransactionId = bankTransactionId;
    this.transactionAmount = transactionAmount;
    this.matchingStatus = MatchingStatus.MATCHED;
    this.matchedBy = MatchedBy.SYSTEM;
    this.matchedAt = LocalDateTime.now();
  }

  public void linkDeposit(Long bankTransactionId, BigDecimal transactionAmount) {
    this.bankTransactionId = bankTransactionId;
    this.transactionAmount = transactionAmount;
    // 금액 불일치: 입금 정보만 기록하고 TBC 유지
  }

  public void markFailed() {
    this.matchingStatus = MatchingStatus.FAILED;
  }
}
