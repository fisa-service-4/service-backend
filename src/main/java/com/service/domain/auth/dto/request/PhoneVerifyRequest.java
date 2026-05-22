package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "휴대폰 인증 검증 요청")
public class PhoneVerifyRequest {

  @Schema(description = "휴대폰 번호 (숫자만)", example = "01012341234")
  @NotBlank
  private String phoneNumber;

  @Schema(description = "인증번호 6자리", example = "123456")
  @NotBlank
  private String code;
}
