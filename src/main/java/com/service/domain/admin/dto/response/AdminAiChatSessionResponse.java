package com.service.domain.admin.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AdminAiChatSessionResponse {

  private final Long sessionId;
  private final Long userId;
  private final String sessionType;
  private final String lastUserMessage;
  private final LocalDateTime updatedAt;

  public static AdminAiChatSessionResponse of(
      Long sessionId,
      Long userId,
      String sessionType,
      String lastUserMessage,
      LocalDateTime updatedAt) {
    return new AdminAiChatSessionResponse(
        sessionId, userId, sessionType, lastUserMessage, updatedAt);
  }
}
