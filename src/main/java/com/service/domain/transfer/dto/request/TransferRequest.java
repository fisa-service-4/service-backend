package com.service.domain.transfer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransferRequest {

  @NotNull(message = "fromAccountId는 필수입니다.")
  private Long fromAccountId;

  @NotBlank(message = "toBankCode는 필수입니다.")
  private String toBankCode;

  @NotBlank(message = "toAccountNumber는 필수입니다.")
  private String toAccountNumber;

  @NotNull(message = "transferAmount는 필수입니다.")
  @Positive(message = "이체 금액은 0보다 커야 합니다.")
  private BigDecimal transferAmount;
}
