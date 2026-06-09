package com.service.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "PIN 상태 조회 응답")
public class PinStatusResponse {

  @Schema(description = "PIN 잠금 여부", example = "false")
  private Boolean lockedYn;

  @Schema(description = "PIN 실패 횟수", example = "0")
  private Integer failCount;

  public static PinStatusResponse of(Boolean lockedYn, Integer failCount) {
    return PinStatusResponse.builder().lockedYn(lockedYn).failCount(failCount).build();
  }
}
