package com.service.domain.account.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountRoleUpdateResponse {

  private Long accountId;
  private String accountRole;
  private LocalDateTime updatedAt;
}
