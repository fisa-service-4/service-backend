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

  @Schema(description = "현재 활성 세션(로그인) 수", example = "42")
  private Long activeSessionCount;

  @Schema(description = "최근 1분 평균 API 응답 시간 (ms)", example = "120")
  private Long avgApiResponseMs;

  public static DashboardResponse of(
      long todayAiRequests,
      long todayApiCalls,
      long todayErrors,
      long activeSessionCount,
      Long avgApiResponseMs) {
    return DashboardResponse.builder()
        .todayAiRequests(todayAiRequests)
        .todayApiCalls(todayApiCalls)
        .todayErrors(todayErrors)
        .activeSessionCount(activeSessionCount)
        .avgApiResponseMs(avgApiResponseMs)
        .build();
  }
}
