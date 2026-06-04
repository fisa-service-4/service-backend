package com.service.domain.transfer.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResponse {

  private Long transferId;
  private String transferStatus;
  private LocalDateTime requestedAt;
}
