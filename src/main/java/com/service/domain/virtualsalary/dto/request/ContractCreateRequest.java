package com.service.domain.virtualsalary.dto.request;

import com.service.domain.virtualsalary.enumtype.TaxType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class ContractCreateRequest {

  @NotBlank private String clientName;

  @NotNull @Positive private BigDecimal contractAmount;

  @NotNull @FutureOrPresent private LocalDate expectedPaymentDate;

  @NotNull private TaxType taxType;

  private String memo;
}
