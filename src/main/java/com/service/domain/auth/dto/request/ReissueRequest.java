package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "토큰 재발급 요청")
public class ReissueRequest {

  @Schema(description = "리프레시 토큰", example = "jwt-refresh-token")
  @NotBlank
  private String refreshToken;
}
