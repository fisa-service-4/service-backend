package com.service.domain.admin.dto.response;

import com.service.domain.admin.entity.AiUsageLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "AI 로그 응답")
public class AiLogResponse {

  @Schema(description = "AI 사용 로그 ID", example = "1")
  private Long aiUsageLogId;

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "사용자명", example = "홍길동")
  private String userName;

  @Schema(description = "세션 ID", example = "10")
  private Long sessionId;

  @Schema(description = "사용 모델명", example = "qwen3-32b")
  private String modelName;

  @Schema(description = "요청 유형", example = "CHAT")
  private String requestType;

  @Schema(description = "응답 시간 (ms)", example = "1230")
  private Long responseTimeMs;

  @Schema(description = "성공 여부", example = "true")
  private Boolean successYn;

  @Schema(description = "생성 시각", example = "2026-05-17T10:00:00")
  private LocalDateTime createdAt;

  public static AiLogResponse of(AiUsageLog log, String userName) {
    return AiLogResponse.builder()
        .aiUsageLogId(log.getAiUsageLogId())
        .userId(log.getUserId())
        .userName(userName)
        .sessionId(log.getSessionId())
        .modelName(log.getModelName())
        .requestType(log.getRequestType())
        .responseTimeMs(log.getResponseTimeMs())
        .successYn(log.getSuccessYn())
        .createdAt(log.getCreatedAt())
        .build();
  }
}
