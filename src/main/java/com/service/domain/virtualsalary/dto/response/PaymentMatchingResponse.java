package com.service.domain.virtualsalary.dto.response;

import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentMatchingResponse {

  private Long matchingId;
  private Long contractId;
  private Long bankTransactionId;
  private MatchingStatus matchingStatus;
  private MatchedBy matchedBy;
  private LocalDateTime matchedAt;
}
