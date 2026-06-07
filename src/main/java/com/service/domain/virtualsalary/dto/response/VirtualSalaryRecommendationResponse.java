package com.service.domain.virtualsalary.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualSalaryRecommendationResponse {

  private BigDecimal recommendedTargetSalary;
  private BigDecimal recommendedEmergencyAmount;
  private BigDecimal recommendedInvestmentAmount;
  private String summary;
}
