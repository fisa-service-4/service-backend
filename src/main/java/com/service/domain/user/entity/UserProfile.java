package com.service.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

  public void update(Boolean freelancerYn, String jobType) {
    if (freelancerYn != null) this.freelancerYn = freelancerYn;
    if (jobType != null) this.jobType = jobType;
  }
}
