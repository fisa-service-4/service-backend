package com.service.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PhoneVerifyRequest {

  @NotBlank private String phoneNumber;

  @NotBlank private String code;
}
