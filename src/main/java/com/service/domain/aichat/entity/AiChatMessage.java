package com.service.domain.aichat.entity;

import com.service.domain.aichat.enumtype.MessageRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "ai_chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "message_id")
  private Long messageId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private AiChatSession session;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private MessageRole role;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "intent")
  private String intent;

  @Column(name = "action_type")
  private String actionType;

  @Column(name = "action_confirmed_yn", nullable = false)
  private Boolean actionConfirmedYn;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    if (this.actionConfirmedYn == null) {
      this.actionConfirmedYn = false;
    }
  }
}
