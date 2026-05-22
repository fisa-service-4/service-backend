package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "PIN 변경 요청")
public class PinChangeRequest {

  @Schema(description = "기존 PIN (6자리)", example = "123456")
  @NotBlank
  @Size(min = 6, max = 6, message = "PIN은 6자리여야 합니다.")
  private String currentPin;

  @Schema(description = "새 PIN (6자리)", example = "654321")
  @NotBlank
  @Size(min = 6, max = 6, message = "PIN은 6자리여야 합니다.")
  private String newPin;
}
