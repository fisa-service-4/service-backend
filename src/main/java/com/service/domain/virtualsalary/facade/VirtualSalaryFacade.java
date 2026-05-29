package com.service.domain.virtualsalary.facade;

import com.service.domain.virtualsalary.dto.response.ContractListResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalaryDashboardResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySummaryResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySummaryResponse.CalendarItem;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySummaryResponse.ContractSummary;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySummaryResponse.DashboardSummary;
import com.service.domain.virtualsalary.service.ContractService;
import com.service.domain.virtualsalary.service.VirtualSalaryDashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VirtualSalaryFacade {

  private final VirtualSalaryDashboardService virtualSalaryDashboardService;
  private final ContractService contractService;

  public VirtualSalarySummaryResponse getSummary(Long userId) {
    VirtualSalaryDashboardResponse dashboard = virtualSalaryDashboardService.getDashboard(userId);
    List<ContractListResponse> contracts = contractService.getContracts(userId, LocalDate.now());

    BigDecimal monthlyExpectedIncome =
        contracts.stream()
            .map(ContractListResponse::getActualIncome)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<ContractSummary> contractSummaries =
        contracts.stream()
            .map(
                c ->
                    ContractSummary.builder()
                        .contractId(c.getContractId())
                        .clientName(c.getClientName())
                        .expectedPaymentDate(c.getExpectedPaymentDate())
                        .actualIncome(c.getActualIncome())
                        .contractStatus(c.getContractStatus())
                        .build())
            .toList();

    List<CalendarItem> calendarData = buildCalendarData(contracts);

    return VirtualSalarySummaryResponse.builder()
        .dashboard(
            DashboardSummary.builder()
                .targetSalary(dashboard.getTargetSalary())
                .currentBalance(dashboard.getCurrentBalance())
                .progressRate(dashboard.getProgressRate())
                .dday(dashboard.getDday())
                .build())
        .monthlyExpectedIncome(monthlyExpectedIncome)
        .contracts(contractSummaries)
        .calendarData(calendarData)
        .build();
  }

  private List<CalendarItem> buildCalendarData(List<ContractListResponse> contracts) {
    Map<LocalDate, BigDecimal> grouped =
        contracts.stream()
            .collect(
                Collectors.groupingBy(
                    ContractListResponse::getExpectedPaymentDate,
                    Collectors.reducing(
                        BigDecimal.ZERO, ContractListResponse::getActualIncome, BigDecimal::add)));

    return grouped.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> CalendarItem.builder().date(e.getKey()).amount(e.getValue()).build())
        .toList();
  }
}
