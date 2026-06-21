package com.service.domain.account.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountTransactionResponse {

  private Long transactionId;
  private String transactionType;
  private String transactionCategory;
  private BigDecimal amount;
  private BigDecimal balanceAfter;
  private String merchantName;
  private LocalDateTime transactionOccurredAt;
}
