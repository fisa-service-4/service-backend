package com.service.domain.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "api_call_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiCallLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "api_log_id")
  private Long apiLogId;

  @Column(name = "trace_id", length = 255)
  private String traceId;

  @Column(name = "service_name", nullable = false, length = 100)
  private String serviceName;

  @Column(name = "api_name", nullable = false, length = 255)
  private String apiName;

  @Column(name = "http_method", nullable = false, length = 20)
  private String httpMethod;

  @Column(name = "response_code", nullable = false, length = 20)
  private String responseCode;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;
}
