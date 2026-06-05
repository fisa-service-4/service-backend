package com.service.global.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.service.domain.virtualsalary.dto.request.AiRecommendationRequest;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

  private final RestTemplate restTemplate;

  @Value("${ai.server.url}")
  private String aiServerUrl;

  public RecommendationResult getVirtualSalaryRecommendation(AiRecommendationRequest request) {
    String url = aiServerUrl + "/virtual-salary/recommend";
    try {
      ResponseEntity<RecommendationWrapper> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(request), RecommendationWrapper.class);

      RecommendationWrapper body = response.getBody();
      if (body == null || !body.isSuccess() || body.getData() == null) {
        log.warn("AI 서버 응답 실패: success={}", body != null && body.isSuccess());
        throw new BusinessException(ErrorCode.AI_001);
      }
      return body.getData();
    } catch (ResourceAccessException e) {
      log.error("AI 서버 연결 실패: {}", e.getMessage());
      throw new BusinessException(ErrorCode.AI_002);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("AI 응답 처리 실패: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.AI_001);
    }
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
  public static class RecommendationResult {
    private BigDecimal recommendedTargetSalary;
    private BigDecimal recommendedEmergencyAmount;
    private BigDecimal recommendedInvestmentAmount;
    private String summary;
  }
}
