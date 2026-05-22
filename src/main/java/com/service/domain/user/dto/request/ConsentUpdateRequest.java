package com.service.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "알림 동의 수정 요청")
public class ConsentUpdateRequest {

  @Schema(description = "알림 동의 여부", example = "false")
  @NotNull
  private Boolean notificationConsentYn;
}
