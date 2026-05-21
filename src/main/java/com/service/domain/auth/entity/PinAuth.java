package com.service.domain.auth.entity;

import com.service.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "pin_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PinAuth {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "pin_hash", nullable = false, length = 255)
  private String pinHash;

  @Column(name = "fail_count", nullable = false)
  private int failCount;

  @Column(name = "locked_yn", nullable = false)
  private Boolean lockedYn;

  @Column(name = "locked_at")
  private LocalDateTime lockedAt;

  @Column(name = "pin_changed_at")
  private LocalDateTime pinChangedAt;

  public void fail() {
    this.failCount++;
    if (this.failCount >= 5) {
      this.lockedYn = true;
      this.lockedAt = LocalDateTime.now();
    }
  }

  public void changePin(String newPinHash) {
    this.pinHash = newPinHash;
    this.failCount = 0;
    this.lockedYn = false;
    this.lockedAt = null;
    this.pinChangedAt = LocalDateTime.now();
  }

  public void resetFailCount() {
    this.failCount = 0;
  }
}
