package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;

public interface ContractService {

  ContractCreateResponse createContract(Long userId, ContractCreateRequest request);
}
