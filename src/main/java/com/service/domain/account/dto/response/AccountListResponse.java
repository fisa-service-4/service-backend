package com.service.domain.account.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountListResponse {

  private Long accountId;
  private String bankCode;
  private String accountNumber;
  private String accountName;
  private BigDecimal balance;
  private String accountStatus;
  private String accountRole;
}
