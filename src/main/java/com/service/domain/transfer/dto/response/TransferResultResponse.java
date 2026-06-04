package com.service.domain.transfer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResultResponse {

  private Long transferId;
  private Long fromAccountId;
  private String toBankCode;
  private String toAccountNumber;
  private BigDecimal transferAmount;
  private String transferStatus;
  private String failureReason;
  private LocalDateTime requestedAt;
  private LocalDateTime completedAt;
}
