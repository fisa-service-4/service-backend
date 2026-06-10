package com.service.domain.aichat.dto.response;

import com.service.domain.aichat.entity.AiChatSession;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionListResponse {

  private Long sessionId;
  private String title;
  private String status;
  private LocalDateTime createdAt;

  public static SessionListResponse from(AiChatSession session) {
    return SessionListResponse.builder()
        .sessionId(session.getSessionId())
        .title(session.getTitle())
        .status(session.getStatus().name())
        .createdAt(session.getCreatedAt())
        .build();
  }
}
