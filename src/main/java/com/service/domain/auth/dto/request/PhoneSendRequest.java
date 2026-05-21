package com.service.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PhoneSendRequest {

  @NotBlank private String phoneNumber;
}
