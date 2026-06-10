package com.service.domain.aichat.dto.response;

import com.service.domain.aichat.entity.AiChatSession;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionCreateResponse {

  private Long sessionId;
  private String status;

  public static SessionCreateResponse from(AiChatSession session) {
    return SessionCreateResponse.builder()
        .sessionId(session.getSessionId())
        .status(session.getStatus().name())
        .build();
  }
}
