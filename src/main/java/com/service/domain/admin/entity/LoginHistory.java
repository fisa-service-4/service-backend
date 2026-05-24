package com.service.domain.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "login_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoginHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "login_history_id")
  private Long loginHistoryId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "login_type", nullable = false, length = 20)
  private String loginType;

  @Column(name = "ip_address", nullable = false, length = 50)
  private String ipAddress;

  @Column(name = "device_info", length = 255)
  private String deviceInfo;

  @Column(name = "fail_reason", length = 255)
  private String failReason;

  @Column(name = "logged_at", nullable = false)
  private LocalDateTime loggedAt;
}
