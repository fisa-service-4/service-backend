package com.service.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "토큰 재발급 응답")
public class TokenResponse {

  @Schema(description = "새로운 액세스 토큰", example = "new-access-token")
  private String accessToken;

  @Schema(description = "리프레시 토큰", example = "jwt-refresh-token")
  private String refreshToken;
}
