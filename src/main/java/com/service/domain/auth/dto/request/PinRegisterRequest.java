package com.service.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PinRegisterRequest {

  @NotBlank
  @Size(min = 6, max = 6, message = "PIN은 6자리여야 합니다.")
  private String pin;
}
