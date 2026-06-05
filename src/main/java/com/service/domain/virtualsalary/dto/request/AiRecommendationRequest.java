package com.service.domain.virtualsalary.dto.request;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequest {

  private Long userId;
  private BigDecimal targetSalary;
  private BigDecimal currentBalance;
  private BigDecimal monthlyExpectedIncome;
  private BigDecimal emergencyTargetAmount;
  private BigDecimal emergencyAmount;
  private BigDecimal investmentAmount;
}
