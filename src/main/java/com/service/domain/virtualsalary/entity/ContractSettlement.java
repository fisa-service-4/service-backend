package com.service.domain.virtualsalary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "CONTRACT_SETTLEMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ContractSettlement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "settlement_id")
  private Long settlementId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  @Column(name = "tax_rate", nullable = false)
  private BigDecimal taxRate;

  @Column(name = "deducted_amount", nullable = false)
  private BigDecimal deductedAmount;

  @Column(name = "actual_income", nullable = false)
  private BigDecimal actualIncome;

  @Column(name = "calculated_at", nullable = false)
  private LocalDateTime calculatedAt;

  public void assignContract(Contract contract) {
    this.contract = contract;
  }

  @PrePersist
  public void prePersist() {
    this.calculatedAt = LocalDateTime.now();
  }
}
