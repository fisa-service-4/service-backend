package com.service.domain.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "ai_usage_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiUsageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ai_usage_log_id")
  private Long aiUsageLogId;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "session_id")
  private Long sessionId;

  @Column(name = "model_name", nullable = false, length = 100)
  private String modelName;

  @Column(name = "response_time_ms")
  private Long responseTimeMs;

  @Column(name = "request_type", length = 50)
  private String requestType;

  @Column(name = "success_yn", nullable = false)
  private Boolean successYn;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
