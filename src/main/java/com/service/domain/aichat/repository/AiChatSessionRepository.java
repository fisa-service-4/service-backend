package com.service.domain.aichat.repository;

import com.service.domain.aichat.entity.AiChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

  Page<AiChatSession> findByUserId(Long userId, Pageable pageable);
}
