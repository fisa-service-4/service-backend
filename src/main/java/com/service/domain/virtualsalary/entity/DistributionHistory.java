package com.service.domain.virtualsalary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "distribution_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DistributionHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "distribution_id")
  private Long distributionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "contract_id", nullable = false)
  private Long contractId;

  @Column(name = "matching_id", nullable = false)
  private Long matchingId;

  @Column(name = "salary_reserved_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal salaryReservedAmount;

  @Column(name = "emergency_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal emergencyAmount;

  @Column(name = "investment_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal investmentAmount;

  @Column(name = "living_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal livingAmount;

  @Column(name = "distributed_at", nullable = false)
  private LocalDateTime distributedAt;

  @PrePersist
  public void prePersist() {
    this.distributedAt = LocalDateTime.now();
  }
}
