package com.service.domain.auth.dto.response;

import com.service.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "회원가입 응답")
public class SignupResponse {

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "이메일", example = "user@test.com")
  private String email;

  @Schema(description = "사용자 이름", example = "홍길동")
  private String userName;

  public static SignupResponse of(User user) {
    return SignupResponse.builder()
        .userId(user.getUserId())
        .email(user.getEmail())
        .userName(user.getUserName())
        .build();
  }
}
