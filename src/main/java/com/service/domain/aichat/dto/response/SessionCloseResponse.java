package com.service.domain.aichat.dto.response;

import com.service.domain.aichat.entity.AiChatSession;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionCloseResponse {

  private Long sessionId;
  private String status;

  public static SessionCloseResponse from(AiChatSession session) {
    return SessionCloseResponse.builder()
        .sessionId(session.getSessionId())
        .status(session.getStatus().name())
        .build();
  }
}
