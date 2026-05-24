package com.service.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 대시보드 응답")
public class DashboardResponse {

  @Schema(description = "오늘 AI 요청 수", example = "1245")
  private Long todayAiRequests;

  @Schema(description = "오늘 API 호출 수", example = "8541")
  private Long todayApiCalls;

  @Schema(description = "오늘 오류 수", example = "23")
  private Long todayErrors;

  public static DashboardResponse of(long todayAiRequests, long todayApiCalls, long todayErrors) {
    return DashboardResponse.builder()
        .todayAiRequests(todayAiRequests)
        .todayApiCalls(todayApiCalls)
        .todayErrors(todayErrors)
        .build();
  }
}
