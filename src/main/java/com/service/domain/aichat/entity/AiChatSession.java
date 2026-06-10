package com.service.domain.aichat.entity;

import com.service.domain.aichat.enumtype.SessionStatus;
import com.service.domain.aichat.enumtype.SessionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "ai_chat_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiChatSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "session_id")
  private Long sessionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "title")
  private String title;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "session_type", nullable = false)
  private SessionType sessionType = SessionType.CHAT;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SessionStatus status = SessionStatus.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.status == null) {
      this.status = SessionStatus.ACTIVE;
    }
    if (this.sessionType == null) {
      this.sessionType = SessionType.CHAT;
    }
  }

  public void close() {
    this.status = SessionStatus.CLOSED;
    this.updatedAt = LocalDateTime.now();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }
}
