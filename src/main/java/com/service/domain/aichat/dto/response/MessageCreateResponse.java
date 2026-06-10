package com.service.domain.aichat.dto.response;

import com.service.domain.aichat.entity.AiChatMessage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageCreateResponse {

  private Long messageId;
  private String role;
  private String intent;
  private String content;
  private Boolean actionRequired;

  public static MessageCreateResponse from(AiChatMessage message) {
    return MessageCreateResponse.builder()
        .messageId(message.getMessageId())
        .role(message.getRole().name())
        .intent(message.getIntent())
        .content(message.getContent())
        .actionRequired(message.getActionType() != null && !message.getActionConfirmedYn())
        .build();
  }
}
