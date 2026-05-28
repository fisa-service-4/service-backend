package com.service.domain.virtualsalary.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractCreateResponse {

  private Long contractId;

  private BigDecimal contractAmount;

  private BigDecimal deductedAmount;

  private BigDecimal actualIncome;
}
