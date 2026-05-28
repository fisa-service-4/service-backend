package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;
import com.service.domain.virtualsalary.dto.response.ContractDetailResponse;
import com.service.domain.virtualsalary.dto.response.ContractListResponse;
import com.service.domain.virtualsalary.entity.Contract;
import com.service.domain.virtualsalary.entity.ContractSettlement;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.repository.ContractRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractServiceImpl implements ContractService {

  private final ContractRepository contractRepository;

  @Override
  public ContractCreateResponse createContract(Long userId, ContractCreateRequest request) {

    BigDecimal taxRate = request.getTaxType().getRate();

    BigDecimal deductedAmount =
        request.getContractAmount().multiply(taxRate).setScale(0, RoundingMode.DOWN);

    BigDecimal actualIncome = request.getContractAmount().subtract(deductedAmount);

    Contract contract =
        Contract.builder()
            .userId(userId)
            .clientName(request.getClientName())
            .contractAmount(request.getContractAmount())
            .taxType(request.getTaxType())
            .taxRate(taxRate)
            .expectedPaymentDate(request.getExpectedPaymentDate())
            .contractStatus(ContractStatus.PENDING)
            .memo(request.getMemo())
            .build();

    ContractSettlement settlement =
        ContractSettlement.builder()
            .contract(contract)
            .taxRate(taxRate)
            .deductedAmount(deductedAmount)
            .actualIncome(actualIncome)
            .build();

    contract.assignSettlement(settlement);

    contractRepository.save(contract);

    return ContractCreateResponse.builder()
        .contractId(contract.getContractId())
        .contractAmount(contract.getContractAmount())
        .deductedAmount(deductedAmount)
        .actualIncome(actualIncome)
        .build();
  }

  @Override
  public List<ContractListResponse> getContracts(Long userId, LocalDate date) {

    LocalDate targetDate = date != null ? date : LocalDate.now();

    YearMonth yearMonth = YearMonth.from(targetDate);

    LocalDate startDate = yearMonth.atDay(1);

    LocalDate endDate = yearMonth.atEndOfMonth();

    List<Contract> contracts =
        contractRepository.findAllByUserIdAndExpectedPaymentDateBetween(userId, startDate, endDate);

    return contracts.stream()
        .map(
            contract ->
                ContractListResponse.builder()
                    .contractId(contract.getContractId())
                    .clientName(contract.getClientName())
                    .contractAmount(contract.getContractAmount())
                    .actualIncome(contract.getSettlement().getActualIncome())
                    .expectedPaymentDate(contract.getExpectedPaymentDate())
                    .contractStatus(contract.getContractStatus())
                    .build())
        .toList();
  }

  @Override
  public ContractDetailResponse getContractDetail(Long userId, Long contractId) {

    Contract contract =
        contractRepository
            .findByContractIdAndUserId(contractId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_001));

    return ContractDetailResponse.builder()
        .contractId(contract.getContractId())
        .clientName(contract.getClientName())
        .contractAmount(contract.getContractAmount())
        .taxRate(contract.getTaxRate())
        .deductedAmount(contract.getSettlement().getDeductedAmount())
        .actualIncome(contract.getSettlement().getActualIncome())
        .taxType(contract.getTaxType())
        .expectedPaymentDate(contract.getExpectedPaymentDate())
        .actualPaymentDate(contract.getActualPaymentDate())
        .contractStatus(contract.getContractStatus())
        .memo(contract.getMemo())
        .build();
  }
}
