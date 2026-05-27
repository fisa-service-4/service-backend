package com.service.domain.virtualsalary.entity;

import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "PAYMENT_MATCHING")
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

  @Column(name = "bank_transaction_id", nullable = false)
  private Long bankTransactionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "matching_status", nullable = false)
  private MatchingStatus matchingStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "matched_by", nullable = false)
  private MatchedBy matchedBy;

  @Column(name = "matched_at")
  private LocalDateTime matchedAt;
}
