package com.service.domain.virtualsalary.dto.response;

import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManualMatchingResponse {

  private Long matchingId;
  private MatchingStatus matchingStatus;
  private MatchedBy matchedBy;
  private LocalDateTime matchedAt;
}
