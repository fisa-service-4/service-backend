package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;
import com.service.domain.virtualsalary.entity.Contract;
import com.service.domain.virtualsalary.entity.ContractSettlement;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.repository.ContractRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
}
