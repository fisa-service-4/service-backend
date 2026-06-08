package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "관리자 회원가입 요청")
public class AdminSignupRequest {

  @Schema(description = "이메일 (이메일 형식)", example = "admin@test.com")
  @NotBlank
  @Email
  private String email;

  @Schema(description = "비밀번호 (영문+숫자+특수문자 8자 이상)", example = "Password123!")
  @NotBlank
  @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
      message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.")
  private String password;

  @Schema(description = "관리자 이름", example = "관리자1")
  @NotBlank
  private String userName;
}
