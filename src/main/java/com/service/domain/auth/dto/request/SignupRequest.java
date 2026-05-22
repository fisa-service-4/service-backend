package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "회원가입 요청")
public class SignupRequest {

  @Schema(description = "이메일 (이메일 형식)", example = "user@test.com")
  @NotBlank
  @Email
  private String email;

  @Schema(description = "비밀번호 (영문+숫자+특수문자 8자 이상)", example = "Password123!")
  @NotBlank
  @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
  private String password;

  @Schema(description = "사용자 이름", example = "홍길동")
  @NotBlank
  private String userName;

  @Schema(description = "휴대폰 번호 (숫자만)", example = "01012345678")
  @NotBlank
  private String phoneNumber;

  @Schema(description = "프리랜서 여부", example = "true")
  @NotNull
  private Boolean freelancerYn;

  @Schema(description = "직업 유형", example = "DEVELOPER")
  private String jobType;

  @Schema(description = "서비스 이용 동의 (true 필수)", example = "true")
  @NotNull
  private Boolean termsConsentYn;
}
