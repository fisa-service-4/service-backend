package com.service.global.client;

import com.service.domain.virtualsalary.dto.request.AiRecommendationRequest;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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
              url,
              HttpMethod.POST,
              new HttpEntity<>(request),
              new ParameterizedTypeReference<>() {});

      RecommendationWrapper body = response.getBody();
      if (body == null || !body.isSuccess() || body.getData() == null) {
        throw new BusinessException(ErrorCode.AI_001);
      }
      return body.getData();
    } catch (ResourceAccessException e) {
      throw new BusinessException(ErrorCode.AI_002);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.AI_001);
    }
  }

  @Getter
  @NoArgsConstructor
  static class RecommendationWrapper {
    private boolean success;
    private RecommendationResult data;
  }

  @Getter
  @NoArgsConstructor
  public static class RecommendationResult {
    private BigDecimal recommendedEmergencyAmount;
    private BigDecimal recommendedInvestmentAmount;
    private String summary;
  }
}
