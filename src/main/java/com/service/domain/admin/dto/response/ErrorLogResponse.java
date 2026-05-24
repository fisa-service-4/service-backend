package com.service.domain.admin.dto.response;

import com.service.domain.admin.entity.SystemErrorLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "오류 로그 응답")
public class ErrorLogResponse {

  @Schema(description = "오류 로그 ID", example = "1")
  private Long errorLogId;

  @Schema(description = "서비스명", example = "service-backend")
  private String serviceName;

  @Schema(description = "오류 레벨", example = "ERROR")
  private String errorLevel;

  @Schema(description = "오류 코드", example = "DB_001")
  private String errorCode;

  @Schema(description = "오류 메시지", example = "Connection timeout")
  private String errorMessage;

  @Schema(description = "요청 URI", example = "/api/v1/users/me")
  private String requestUri;

  @Schema(description = "해결 여부", example = "false")
  private Boolean resolvedYn;

  @Schema(description = "해결 시각", example = "2026-05-17T12:00:00")
  private LocalDateTime resolvedAt;

  @Schema(description = "생성 시각", example = "2026-05-17T10:00:00")
  private LocalDateTime createdAt;

  public static ErrorLogResponse of(SystemErrorLog log) {
    return ErrorLogResponse.builder()
        .errorLogId(log.getErrorLogId())
        .serviceName(log.getServiceName())
        .errorLevel(log.getErrorLevel())
        .errorCode(log.getErrorCode())
        .errorMessage(log.getErrorMessage())
        .requestUri(log.getRequestUri())
        .resolvedYn(log.getResolvedYn())
        .resolvedAt(log.getResolvedAt())
        .createdAt(log.getCreatedAt())
        .build();
  }
}
