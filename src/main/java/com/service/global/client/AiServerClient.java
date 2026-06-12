package com.service.global.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.virtualsalary.dto.request.AiRecommendationRequest;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

  private static final String AI_MODEL_NAME = "Qwen3-8B";
  private static final String REQUEST_TYPE = "VIRTUAL_SALARY";
  private static final String REQUEST_TYPE_CHAT = "CHAT";

  private final RestTemplate restTemplate;
  private final AdminLogSaveService adminLogSaveService;

  @Value("${ai.server.url}")
  private String aiServerUrl;

  public RecommendationResult getVirtualSalaryRecommendation(AiRecommendationRequest request) {
    String url = aiServerUrl + "/virtual-salary/recommend";
    Long userId = getCurrentUserId();
    long startTime = System.currentTimeMillis();
    boolean success = false;

    try {
      ResponseEntity<RecommendationWrapper> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(request), RecommendationWrapper.class);

      RecommendationWrapper body = response.getBody();
      if (body == null || !body.isSuccess() || body.getData() == null) {
        log.warn("AI 서버 응답 실패: success={}", body != null && body.isSuccess());
        throw new BusinessException(ErrorCode.AI_001);
      }
      success = true;
      return body.getData();
    } catch (ResourceAccessException e) {
      log.error("AI 서버 연결 실패: {}", e.getMessage());
      throw new BusinessException(ErrorCode.AI_002);
    } catch (RestClientResponseException e) {
      log.error("AI 서버 에러 응답 (status={}, body={})", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BusinessException(ErrorCode.AI_001);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("AI 응답 처리 실패: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.AI_001);
    } finally {
      long durationMs = System.currentTimeMillis() - startTime;
      try {
        adminLogSaveService.saveAiUsageLog(
            userId, AI_MODEL_NAME, durationMs, REQUEST_TYPE, success);
      } catch (Exception e) {
        log.error("AI 사용 로그 비동기 저장 요청 실패", e);
      }
    }
  }

  private Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Long userId) {
      return userId;
    }
    return null;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class RecommendationWrapper {
    private boolean success;
    private RecommendationResult data;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class ChatRunWrapper {
    private boolean success;
    private ChatRunResult data;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RecommendationResult {
    private BigDecimal recommendedTargetSalary;
    private BigDecimal recommendedEmergencyAmount;
    private BigDecimal recommendedInvestmentAmount;
    private String summary;
  }

  public ChatRunResult runChatAgent(Long sessionId, String message, Boolean isPin, String authorization) {
    String url = aiServerUrl + "/chat/run";
    Long userId = getCurrentUserId();
    long startTime = System.currentTimeMillis();
    boolean success = false;

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", authorization);
    ChatRunRequest requestBody = new ChatRunRequest(sessionId, message, isPin != null && isPin);

    try {
      ResponseEntity<ChatRunWrapper> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), ChatRunWrapper.class);
      ChatRunWrapper body = response.getBody();
      if (body == null || !body.isSuccess() || body.getData() == null) {
        log.warn("AI 서버 응답 실패: success={}", body != null && body.isSuccess());
        throw new BusinessException(ErrorCode.AI_001);
      }
      ChatRunResult result = body.getData();
      success = true;
      return result;
    } catch (ResourceAccessException e) {
      log.error("AI 서버 연결 실패: {}", e.getMessage());
      throw new BusinessException(ErrorCode.AI_002);
    } catch (RestClientResponseException e) {
      log.error(
          "AI 서버 에러 응답 (status={}, body={})",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new BusinessException(ErrorCode.AI_001);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("AI 채팅 응답 처리 실패: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.AI_001);
    } finally {
      long durationMs = System.currentTimeMillis() - startTime;
      try {
        adminLogSaveService.saveAiUsageLog(
            userId, AI_MODEL_NAME, durationMs, REQUEST_TYPE_CHAT, success);
      } catch (Exception e) {
        log.error("AI 사용 로그 비동기 저장 요청 실패", e);
      }
    }
  }

  public void triggerPipeline(Long userId) {
    String url = aiServerUrl + "/admin/pipeline/run?user_id=" + userId;
    try {
      restTemplate.postForEntity(url, null, Void.class);
      log.info("[Pipeline] 트리거 성공 (userId={})", userId);
    } catch (Exception e) {
      log.warn("[Pipeline] 트리거 실패 (userId={}): {}", userId, e.getMessage());
    }
  }

  @Getter
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ChatRunRequest {
    private Long sessionId;
    private String message;
    private Boolean isPin;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ChatRunResult {
    private Long messageId;
    private String role;
    private String intent;
    private String content;
    private Boolean actionRequired;
    private Boolean requirePin;
  }
}
