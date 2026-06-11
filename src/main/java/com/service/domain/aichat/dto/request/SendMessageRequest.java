package com.service.domain.aichat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendMessageRequest {

  @NotNull
  private Long sessionId;

  @NotBlank
  private String message;
}
