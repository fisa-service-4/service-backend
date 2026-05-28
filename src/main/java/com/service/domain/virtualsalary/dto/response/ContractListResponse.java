package com.service.domain.virtualsalary.dto.response;

import com.service.domain.virtualsalary.enumtype.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractListResponse {

  private Long contractId;

  private String clientName;

  private BigDecimal contractAmount;

  private BigDecimal actualIncome;

  private LocalDate expectedPaymentDate;

  private ContractStatus contractStatus;
}
