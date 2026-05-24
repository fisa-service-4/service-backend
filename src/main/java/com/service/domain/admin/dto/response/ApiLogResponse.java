package com.service.domain.admin.dto.response;

import com.service.domain.admin.entity.ApiCallLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "API 로그 응답")
public class ApiLogResponse {

  @Schema(description = "API 로그 ID", example = "1")
  private Long apiLogId;

  @Schema(description = "Trace ID", example = "trace-abc-123")
  private String traceId;

  @Schema(description = "서비스명", example = "service-backend")
  private String serviceName;

  @Schema(description = "API 이름", example = "/api/v1/users/me")
  private String apiName;

  @Schema(description = "HTTP Method", example = "GET")
  private String httpMethod;

  @Schema(description = "응답 코드", example = "200")
  private String responseCode;

  @Schema(description = "응답 시간 (ms)", example = "120")
  private Long durationMs;

  @Schema(description = "요청 시각", example = "2026-05-17T10:00:00")
  private LocalDateTime requestedAt;

  public static ApiLogResponse of(ApiCallLog log) {
    return ApiLogResponse.builder()
        .apiLogId(log.getApiLogId())
        .traceId(log.getTraceId())
        .serviceName(log.getServiceName())
        .apiName(log.getApiName())
        .httpMethod(log.getHttpMethod())
        .responseCode(log.getResponseCode())
        .durationMs(log.getDurationMs())
        .requestedAt(log.getRequestedAt())
        .build();
  }
}
