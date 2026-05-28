package com.service.domain.virtualsalary.dto.response;

import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractDetailResponse {

  private Long contractId;

  private String clientName;

  private BigDecimal contractAmount;

  private BigDecimal taxRate;

  private BigDecimal deductedAmount;

  private BigDecimal actualIncome;

  private TaxType taxType;

  private LocalDate expectedPaymentDate;

  private LocalDate actualPaymentDate;

  private ContractStatus contractStatus;

  private String memo;
}
