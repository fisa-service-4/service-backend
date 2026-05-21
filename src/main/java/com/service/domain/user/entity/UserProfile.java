package com.service.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

// 회원가입, 프로필 수정
public class UserProfile {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "freelancer_yn", nullable = false)
  private Boolean freelancerYn;

  @Column(name = "job_type", length = 100)
  private String jobType;

  public void update(String jobType) {
    this.jobType = jobType;
  }
}
