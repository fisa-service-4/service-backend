package com.service.domain.virtualsalary.dto.request;

import com.service.domain.virtualsalary.enumtype.VirtualSalaryCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VirtualSalarySettingRequest {

  @NotNull(message = "가상월급 금액은 필수입니다.")
  @DecimalMin(value = "0.01", message = "가상월급 금액은 0보다 커야 합니다.")
  private BigDecimal targetSalary;

  @NotNull(message = "월급일은 필수입니다.")
  @Min(value = 1, message = "월급일은 1 이상이어야 합니다.")
  @Max(value = 31, message = "월급일은 31 이하이어야 합니다.")
  private Integer payday;

  @DecimalMin(value = "0.01", message = "비상금 목표 금액은 0보다 커야 합니다.")
  private BigDecimal emergencyTargetAmount;

  @DecimalMin(value = "0.01", message = "투자 이체 금액은 0보다 커야 합니다.")
  private BigDecimal investmentAmount;

  @DecimalMin(value = "0.01", message = "비상금 이체 금액은 0보다 커야 합니다.")
  private BigDecimal emergencyAmount;

  private List<VirtualSalaryCategory> priorityOrder;
}
