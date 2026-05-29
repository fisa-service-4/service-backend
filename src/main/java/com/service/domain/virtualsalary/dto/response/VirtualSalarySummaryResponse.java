package com.service.domain.virtualsalary.dto.response;

import com.service.domain.virtualsalary.enumtype.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VirtualSalarySummaryResponse {

  private DashboardSummary dashboard;
  private BigDecimal monthlyExpectedIncome;
  private List<ContractSummary> contracts;
  private List<CalendarItem> calendarData;

  @Getter
  @Builder
  public static class DashboardSummary {
    private BigDecimal targetSalary;
    private BigDecimal currentBalance;
    private BigDecimal progressRate;
    private Long dday;
  }

  @Getter
  @Builder
  public static class ContractSummary {
    private Long contractId;
    private String clientName;
    private LocalDate expectedPaymentDate;
    private BigDecimal actualIncome;
    private ContractStatus contractStatus;
  }

  @Getter
  @Builder
  public static class CalendarItem {
    private LocalDate date;
    private BigDecimal amount;
  }
}
