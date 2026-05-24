package com.service.domain.admin.dto.request;

import com.service.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "사용자 상태 변경 요청")
public class UserStatusUpdateRequest {

  @Schema(description = "변경할 상태 (ACTIVE / INACTIVE / LOCKED / WITHDRAW)", example = "LOCKED")
  @NotNull
  private User.Status status;
}
