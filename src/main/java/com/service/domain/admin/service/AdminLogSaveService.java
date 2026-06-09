package com.service.domain.admin.service;

import com.service.domain.admin.entity.AiUsageLog;
import com.service.domain.admin.entity.ApiCallLog;
import com.service.domain.admin.entity.SystemErrorLog;
import com.service.domain.admin.repository.AiUsageLogRepository;
import com.service.domain.admin.repository.ApiCallLogRepository;
import com.service.domain.admin.repository.SystemErrorLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogSaveService {

  @Value("${spring.application.name:service-backend}")
  private String serviceName;

  private final SystemErrorLogRepository systemErrorLogRepository;
  private final AiUsageLogRepository aiUsageLogRepository;
  private final ApiCallLogRepository apiCallLogRepository;

  @Async("logTaskExecutor")
  @Transactional("logTransactionManager")
  public void saveSystemErrorLog(
      String traceId,
      String errorLevel,
      String errorCode,
      String errorMessage,
      String requestUri,
      Long userId) {
    try {
      systemErrorLogRepository.save(
          SystemErrorLog.builder()
              .traceId(traceId)
              .userId(userId)
              .serviceName(serviceName)
              .errorLevel(errorLevel)
              .errorCode(errorCode)
              .errorMessage(errorMessage)
              .requestUri(requestUri)
              .resolvedYn(false)
              .createdAt(LocalDateTime.now())
              .build());
    } catch (Exception e) {
      log.error("시스템 에러 로그 저장 실패: {}", e.getMessage());
    }
  }

  @Async("logTaskExecutor")
  @Transactional("logTransactionManager")
  public void resolveLoginFailureLogs(Long userId) {
    try {
      systemErrorLogRepository.resolveAllByUserIdAndErrorCode(
          userId, "AUTH_003", LocalDateTime.now());
    } catch (Exception e) {
      log.error("로그인 성공 시 AUTH_003 로그 자동 해결 실패: {}", e.getMessage());
    }
  }

  @Async("logTaskExecutor")
  public void saveAiUsageLog(
      Long userId, String modelName, Long responseTimeMs, String requestType, boolean successYn) {
    try {
      aiUsageLogRepository.save(
          AiUsageLog.builder()
              .userId(userId)
              .modelName(modelName)
              .responseTimeMs(responseTimeMs)
              .requestType(requestType)
              .successYn(successYn)
              .createdAt(LocalDateTime.now())
              .build());
    } catch (Exception e) {
      log.error("AI 사용 로그 저장 실패: {}", e.getMessage());
    }
  }

  @Async("logTaskExecutor")
  public void saveApiCallLog(
      String traceId,
      String apiName,
      String httpMethod,
      String responseCode,
      Long durationMs,
      LocalDateTime requestedAt) {
    try {
      apiCallLogRepository.save(
          ApiCallLog.builder()
              .traceId(traceId)
              .serviceName(serviceName)
              .apiName(apiName)
              .httpMethod(httpMethod)
              .responseCode(responseCode)
              .durationMs(durationMs)
              .requestedAt(requestedAt)
              .build());
    } catch (Exception e) {
      log.error("API 호출 로그 저장 실패: {}", e.getMessage());
    }
  }
}
