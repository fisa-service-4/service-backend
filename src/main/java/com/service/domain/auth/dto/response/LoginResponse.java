package com.service.domain.auth.dto.response;

import com.service.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

  private String accessToken;
  private String refreshToken;
  private Long userId;
  private String userName;
  private String role;

  public static LoginResponse of(String accessToken, String refreshToken, User user) {
    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .userId(user.getUserId())
        .userName(user.getUserName())
        .role(user.getRole().name())
        .build();
  }
}
