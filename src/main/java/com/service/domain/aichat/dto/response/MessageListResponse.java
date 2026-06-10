package com.service.domain.aichat.dto.response;

import com.service.domain.aichat.entity.AiChatMessage;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageListResponse {

  private Long messageId;
  private String role;
  private String intent;
  private String content;
  private Boolean actionRequired;
  private LocalDateTime createdAt;

  public static MessageListResponse from(AiChatMessage message) {
    return MessageListResponse.builder()
        .messageId(message.getMessageId())
        .role(message.getRole().name())
        .intent(message.getIntent())
        .content(message.getContent())
        .actionRequired(message.getActionType() != null && !message.getActionConfirmedYn())
        .createdAt(message.getCreatedAt())
        .build();
  }
}
