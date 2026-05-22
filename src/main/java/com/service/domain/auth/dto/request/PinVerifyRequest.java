package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "PIN 검증 요청")
public class PinVerifyRequest {

  @Schema(description = "6자리 PIN", example = "123456")
  @NotBlank
  @Size(min = 6, max = 6, message = "PIN은 6자리여야 합니다.")
  private String pin;
}
