package com.service.domain.aichat.dto.request;

import com.service.domain.aichat.enumtype.MessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateMessageRequest {

  @NotNull
  private Long sessionId;

  @NotNull
  private MessageRole role;

  @NotBlank
  private String content;

  private String intent;

  private String actionType;
}
