package com.service.domain.virtualsalary.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VirtualSalaryDashboardResponse {

  private BigDecimal targetSalary;
  private BigDecimal currentBalance;
  private BigDecimal remainAmount;
  private BigDecimal usedAmount;
  private BigDecimal progressRate;
  private Integer payday;
  private Long dday;
}
