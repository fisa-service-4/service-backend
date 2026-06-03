package com.service.domain.virtualsalary.dto.response;

import com.service.domain.virtualsalary.enumtype.VirtualSalaryCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VirtualSalarySettingResponse {

  private BigDecimal targetSalary;
  private Integer payday;
  private BigDecimal emergencyTargetAmount;
  private BigDecimal investmentAmount;
  private BigDecimal emergencyAmount;
  private List<VirtualSalaryCategory> priorityOrder;
  private LocalDateTime updatedAt;
}
