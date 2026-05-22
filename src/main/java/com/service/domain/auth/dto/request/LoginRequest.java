package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "로그인 요청")
public class LoginRequest {

  @Schema(description = "이메일", example = "user@test.com")
  @NotBlank
  @Email
  private String email;

  @Schema(description = "비밀번호", example = "Password123!")
  @NotBlank
  private String password;
}
