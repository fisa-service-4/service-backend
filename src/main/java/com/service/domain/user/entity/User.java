package com.service.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "firebase_uid", nullable = false, unique = true, length = 255)
  private String firebaseUid;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "user_name", nullable = false, length = 100)
  private String userName;

  @Column(name = "phone_number", nullable = false, unique = true, length = 20)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "notification_consent_yn", nullable = false)
  private Boolean notificationConsentYn;

  @Column(name = "terms_consent_yn", nullable = false)
  private Boolean termsConsentYn;

  @Column(name = "mydata_consent_yn", nullable = false)
  private Boolean mydataConsentYn;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public enum Role {
    USER,
    ADMIN
  }

  public enum Status {
    ACTIVE,
    INACTIVE,
    WITHDRAW,
    LOCKED
  }

  public void updateNotificationConsent(Boolean notificationConsentYn) {
    this.notificationConsentYn = notificationConsentYn;
  }

  public void activate() {
    this.status = Status.ACTIVE;
  }

  public void withdraw() {
    this.status = Status.WITHDRAW;
  }
}
