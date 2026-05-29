package com.service.domain.virtualsalary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManualMatchingRequest {

  @NotNull(message = "bankTransactionId는 필수입니다.")
  private Long bankTransactionId;

  @NotNull(message = "matchedBy는 필수입니다.")
  private String matchedBy;
}
