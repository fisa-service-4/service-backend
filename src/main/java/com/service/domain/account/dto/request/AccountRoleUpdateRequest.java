package com.service.domain.account.dto.request;

import com.service.domain.mydata.entity.AccountMapping;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountRoleUpdateRequest {

  @NotNull(message = "accountRole은 필수입니다.")
  private AccountRole accountRole;

  public enum AccountRole {
    DEPOSIT,
    SALARY,
    EMERGENCY,
    STOCK;

    public AccountMapping.MappingType toMappingType() {
      return switch (this) {
        case DEPOSIT -> AccountMapping.MappingType.INCOME;
        case SALARY -> AccountMapping.MappingType.SALARY;
        case EMERGENCY -> AccountMapping.MappingType.EMERGENCY;
        case STOCK -> AccountMapping.MappingType.STOCK;
      };
    }
  }
}
