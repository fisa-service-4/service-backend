package com.service.domain.ai.repository;

import java.time.LocalDateTime;

public interface AiChatSessionSummary {
  Long getSessionId();

  Long getUserId();

  String getUserName();

  String getEmail();

  String getSessionType();

  Long getMessageCount();

  LocalDateTime getUpdatedAt();
}
