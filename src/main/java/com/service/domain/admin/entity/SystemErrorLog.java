package com.service.domain.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "system_error_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SystemErrorLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "error_log_id")
  private Long errorLogId;

  @Column(name = "trace_id", length = 255)
  private String traceId;

  @Column(name = "service_name", nullable = false, length = 100)
  private String serviceName;

  @Column(name = "error_level", nullable = false, length = 20)
  private String errorLevel;

  @Column(name = "error_code", length = 100)
  private String errorCode;

  @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "request_uri", length = 500)
  private String requestUri;

  @Column(name = "resolved_yn", nullable = false)
  private Boolean resolvedYn;

  @Column(name = "resolved_memo", length = 500)
  private String resolvedMemo;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public void resolve(String memo) {
    this.resolvedYn = true;
    this.resolvedMemo = memo;
    this.resolvedAt = LocalDateTime.now();
  }
}
