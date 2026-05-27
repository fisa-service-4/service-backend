package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;
import com.service.domain.virtualsalary.dto.response.ContractDetailResponse;
import com.service.domain.virtualsalary.dto.response.ContractListResponse;
import java.time.LocalDate;
import java.util.List;

public interface ContractService {

  ContractCreateResponse createContract(Long userId, ContractCreateRequest request);

  List<ContractListResponse> getContracts(Long userId, LocalDate date);

  ContractDetailResponse getContractDetail(Long userId, Long contractId);
}
