package com.service.domain.mydata.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

// 마이데이터 연동 실행
public class AccountMapping {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "mapping_id")
  private Long mappingId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_account_id", nullable = false)
  private LinkedFinancialAccount linkedFinancialAccount;

  @Enumerated(EnumType.STRING)
  @Column(name = "mapping_type", nullable = false)
  private MappingType mappingType;

  public enum MappingType {
    INCOME,
    SALARY,
    STOCK,
    EMERGENCY
  }

  public void updateMappingType(MappingType mappingType) {
    this.mappingType = mappingType;
  }
}
