package com.service.domain.auth.dto.response;

import com.service.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {

  private Long userId;
  private String email;
  private String userName;

  public static SignupResponse of(User user) {
    return SignupResponse.builder()
        .userId(user.getUserId())
        .email(user.getEmail())
        .userName(user.getUserName())
        .build();
  }
}
