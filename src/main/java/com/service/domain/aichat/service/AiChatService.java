package com.service.domain.aichat.service;

import com.service.domain.aichat.dto.request.CreateMessageRequest;
import com.service.domain.aichat.dto.request.CreateSessionRequest;
import com.service.domain.aichat.dto.response.MessageCreateResponse;
import com.service.domain.aichat.dto.response.MessageListResponse;
import com.service.domain.aichat.dto.response.SessionCloseResponse;
import com.service.domain.aichat.dto.response.SessionCreateResponse;
import com.service.domain.aichat.dto.response.SessionListResponse;
import com.service.domain.aichat.entity.AiChatMessage;
import com.service.domain.aichat.entity.AiChatSession;
import com.service.domain.aichat.enumtype.MessageRole;
import com.service.domain.aichat.enumtype.SessionStatus;
import com.service.domain.aichat.enumtype.SessionType;
import com.service.domain.aichat.repository.AiChatMessageRepository;
import com.service.domain.aichat.repository.AiChatSessionRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiChatService {

  private final AiChatSessionRepository sessionRepository;
  private final AiChatMessageRepository messageRepository;

  @Transactional
  public SessionCreateResponse createSession(Long userId, CreateSessionRequest request) {
    AiChatSession session =
        AiChatSession.builder()
            .userId(userId)
            .title(request.getTitle())
            .sessionType(SessionType.CHAT)
            .status(SessionStatus.ACTIVE)
            .build();
    return SessionCreateResponse.from(sessionRepository.save(session));
  }

  public List<SessionListResponse> getSessions(Long userId) {
    return sessionRepository
        .findByUserIdAndStatusOrderByUpdatedAtDesc(userId, SessionStatus.ACTIVE)
        .stream()
        .map(SessionListResponse::from)
        .toList();
  }

  @Transactional
  public MessageCreateResponse createMessage(Long userId, CreateMessageRequest request) {
    AiChatSession session = findSessionByUser(request.getSessionId(), userId);

    if (session.getStatus() == SessionStatus.CLOSED) {
      throw new BusinessException(ErrorCode.AI_004);
    }

    AiChatMessage message =
        AiChatMessage.builder()
            .session(session)
            .role(MessageRole.valueOf(request.getRole()))
            .content(request.getContent())
            .intent(request.getIntent())
            .actionType(request.getActionType())
            .build();

    session.touch();
    return MessageCreateResponse.from(messageRepository.save(message));
  }

  public Page<MessageListResponse> getMessages(Long userId, Long sessionId, Pageable pageable) {
    findSessionByUser(sessionId, userId);
    return messageRepository.findBySessionSessionId(sessionId, pageable).map(MessageListResponse::from);
  }

  @Transactional
  public SessionCloseResponse closeSession(Long userId, Long sessionId) {
    AiChatSession session = findSessionByUser(sessionId, userId);

    if (session.getStatus() == SessionStatus.CLOSED) {
      throw new BusinessException(ErrorCode.AI_004);
    }

    session.close();
    return SessionCloseResponse.from(session);
  }

  private AiChatSession findSessionByUser(Long sessionId, Long userId) {
    AiChatSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AI_003));

    if (!session.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.AI_003);
    }

    return session;
  }
}
