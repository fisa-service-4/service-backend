package com.service.domain.virtualsalary.service;

import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.virtualsalary.dto.request.AiRecommendationRequest;
import com.service.domain.virtualsalary.dto.response.VirtualSalaryRecommendationResponse;
import com.service.domain.virtualsalary.entity.Contract;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.repository.ContractRepository;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.client.AiServerClient;
import com.service.global.client.BankServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VirtualSalaryRecommendationServiceImpl implements VirtualSalaryRecommendationService {

  private final VirtualSalarySettingRepository virtualSalarySettingRepository;
  private final AccountMappingRepository accountMappingRepository;
  private final ContractRepository contractRepository;
  private final BankServerClient bankServerClient;
  private final AiServerClient aiServerClient;

  @Override
  public VirtualSalaryRecommendationResponse getRecommendation(Long userId) {
    VirtualSalarySetting setting =
        virtualSalarySettingRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VIRTUAL_SALARY_001));

    BigDecimal currentBalance = resolveCurrentBalance(userId);
    BigDecimal monthlyExpectedIncome = resolveMonthlyExpectedIncome(userId);

    AiRecommendationRequest request =
        AiRecommendationRequest.builder()
            .userId(userId)
            .targetSalary(setting.getTargetSalary())
            .currentBalance(currentBalance)
            .monthlyExpectedIncome(monthlyExpectedIncome)
            .emergencyTargetAmount(setting.getEmergencyTargetAmount())
            .emergencyAmount(setting.getEmergencyAmount())
            .investmentAmount(setting.getInvestmentAmount())
            .build();

    AiServerClient.RecommendationResult result =
        aiServerClient.getVirtualSalaryRecommendation(request);

    return VirtualSalaryRecommendationResponse.builder()
        .recommendedEmergencyAmount(result.getRecommendedEmergencyAmount())
        .recommendedInvestmentAmount(result.getRecommendedInvestmentAmount())
        .summary(result.getSummary())
        .build();
  }

  private BigDecimal resolveCurrentBalance(Long userId) {
    try {
      return accountMappingRepository
          .findByUserIdAndMappingType(userId, AccountMapping.MappingType.SALARY)
          .map(
              m ->
                  bankServerClient.getAccountBalance(
                      m.getLinkedFinancialAccount().getExternalAccountId()))
          .orElse(BigDecimal.ZERO);
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal resolveMonthlyExpectedIncome(Long userId) {
    YearMonth yearMonth = YearMonth.from(LocalDate.now());
    List<Contract> contracts =
        contractRepository.findAllByUserIdAndExpectedPaymentDateBetween(
            userId, yearMonth.atDay(1), yearMonth.atEndOfMonth());

    return contracts.stream()
        .map(c -> c.getSettlement().getActualIncome())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
