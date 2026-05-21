package com.service.domain.mydata.entity;

import com.service.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "linked_financial_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

// 마이데이터 연동, 연동 목록 조회, 동기화
public class LinkedFinancialAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "linked_account_id")
  private Long linkedAccountId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "institution_type", nullable = false)
  private InstitutionType institutionType;

  @Column(name = "institution_code", nullable = false, length = 30)
  private String institutionCode;

  @Column(name = "external_account_id", nullable = false)
  private Long externalAccountId;

  @Column(name = "account_masking", nullable = false, length = 100)
  private String accountMasking;

  @Column(name = "synced_at")
  private LocalDateTime syncedAt;

  public enum InstitutionType {
    BANK,
    SECURITIES,
    CARD
  }

  // 마이데이터 동기화 요청 API(Post /mydata/sync) 호출 시 동기화 시각 갱신
  public void updateSyncedAt() {
    this.syncedAt = LocalDateTime.now();
  }
}
