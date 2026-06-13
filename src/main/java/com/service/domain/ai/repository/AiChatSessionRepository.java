package com.service.domain.ai.repository;

import com.service.domain.ai.entity.AiChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

  @Query(
      value =
          """
          SELECT s.session_id   AS sessionId,
                 u.user_id      AS userId,
                 u.user_name    AS userName,
                 u.email        AS email,
                 s.session_type AS sessionType,
                 COUNT(m.message_id) AS messageCount,
                 s.updated_at   AS updatedAt
          FROM   ai_chat_session s
                 JOIN users u ON u.user_id = s.user_id
                 LEFT JOIN ai_chat_message m ON m.session_id = s.session_id
          WHERE  (:sessionType IS NULL OR s.session_type = :sessionType)
          GROUP  BY s.session_id, u.user_id, u.user_name, u.email,
                    s.session_type, s.updated_at
          ORDER  BY s.updated_at DESC
          """,
      countQuery =
          """
          SELECT COUNT(*)
          FROM   ai_chat_session s
          WHERE  (:sessionType IS NULL OR s.session_type = :sessionType)
          """,
      nativeQuery = true)
  Page<AiChatSessionSummary> findAllSessions(
      @Param("sessionType") String sessionType, Pageable pageable);
}
