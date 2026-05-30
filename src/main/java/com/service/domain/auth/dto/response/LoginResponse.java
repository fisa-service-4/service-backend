package com.service.domain.auth.dto.response;

import com.service.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답")
public class LoginResponse {

  @Schema(description = "액세스 토큰", example = "jwt-access-token")
  private String accessToken;

  @Schema(description = "리프레시 토큰", example = "jwt-refresh-token")
  private String refreshToken;

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "사용자 이름", example = "홍길동")
  private String userName;

  @Schema(description = "권한", example = "USER")
  private String role;

  @Schema(description = "Firebase UID", example = "firebase-uid-abc123")
  private String firebaseUid;

  public static LoginResponse of(String accessToken, String refreshToken, User user) {
    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .userId(user.getUserId())
        .userName(user.getUserName())
        .role(user.getRole().name())
        .firebaseUid(user.getFirebaseUid())
        .build();
  }
}
