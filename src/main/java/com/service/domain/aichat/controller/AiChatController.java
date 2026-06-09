package com.service.domain.aichat.controller;

import com.service.domain.aichat.dto.request.CreateMessageRequest;
import com.service.domain.aichat.dto.request.CreateSessionRequest;
import com.service.domain.aichat.dto.response.MessageCreateResponse;
import com.service.domain.aichat.dto.response.MessageListResponse;
import com.service.domain.aichat.dto.response.SessionCloseResponse;
import com.service.domain.aichat.dto.response.SessionCreateResponse;
import com.service.domain.aichat.dto.response.SessionListResponse;
import com.service.domain.aichat.service.AiChatService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Chat", description = "AI 채팅 API")
@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

  private final AiChatService aiChatService;

  @Operation(summary = "채팅 세션 생성")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "세션 생성 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @PostMapping("/sessions")
  public ResponseEntity<ApiResponse<SessionCreateResponse>> createSession(
      Authentication authentication, @RequestBody CreateSessionRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(aiChatService.createSession(userId, request)));
  }

  @Operation(summary = "채팅 세션 목록 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "세션 목록 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/sessions")
  public ResponseEntity<ApiResponse<Page<SessionListResponse>>> getSessions(
      Authentication authentication,
      @PageableDefault(size = 20) Pageable pageable) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(aiChatService.getSessions(userId, pageable)));
  }

  @Operation(summary = "메시지 저장")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "메시지 저장 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "AI_004: 이미 종료된 세션"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @PostMapping("/messages")
  public ResponseEntity<ApiResponse<MessageCreateResponse>> createMessage(
      Authentication authentication, @Valid @RequestBody CreateMessageRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(aiChatService.createMessage(userId, request)));
  }

  @Operation(summary = "채팅 메시지 목록 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "메시지 목록 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/sessions/{session_id}/messages")
  public ResponseEntity<ApiResponse<Page<MessageListResponse>>> getMessages(
      Authentication authentication,
      @PathVariable("session_id") Long sessionId,
      @PageableDefault(size = 20) Pageable pageable) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(aiChatService.getMessages(userId, sessionId, pageable)));
  }

  @Operation(summary = "채팅 세션 종료")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "세션 종료 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "AI_004: 이미 종료된 세션"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @DeleteMapping("/sessions/{session_id}")
  public ResponseEntity<ApiResponse<SessionCloseResponse>> closeSession(
      Authentication authentication, @PathVariable("session_id") Long sessionId) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(aiChatService.closeSession(userId, sessionId)));
  }
}
