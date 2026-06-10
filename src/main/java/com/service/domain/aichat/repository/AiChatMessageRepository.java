package com.service.domain.aichat.repository;

import com.service.domain.aichat.entity.AiChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

  Page<AiChatMessage> findBySessionSessionId(Long sessionId, Pageable pageable);
}
