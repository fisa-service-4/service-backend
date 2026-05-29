package com.service.domain.account.service;

import com.service.domain.account.dto.request.AccountRoleUpdateRequest;
import com.service.domain.account.dto.response.AccountListResponse;
import com.service.domain.account.dto.response.AccountRoleUpdateResponse;
import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.entity.LinkedFinancialAccount;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.global.client.BankServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

  private final LinkedFinancialAccountRepository linkedFinancialAccountRepository;
  private final AccountMappingRepository accountMappingRepository;
  private final BankServerClient bankServerClient;

  @Transactional(readOnly = true)
  public List<AccountListResponse> getMyAccounts(Long userId) {
    List<BankServerClient.BankAccountItem> bankAccounts = bankServerClient.getBankAccounts(userId);

    Map<Long, AccountMapping.MappingType> roleMap =
        accountMappingRepository.findByUserId(userId).stream()
            .collect(
                Collectors.toMap(
                    m -> m.getLinkedFinancialAccount().getExternalAccountId(),
                    AccountMapping::getMappingType,
                    (a, b) -> a));

    return bankAccounts.stream()
        .map(
            account ->
                AccountListResponse.builder()
                    .accountId(account.getAccountId())
                    .bankCode(account.getBankCode())
                    .accountNumber(account.getAccountNumber())
                    .accountName(account.getAccountName())
                    .balance(account.getBalance())
                    .accountStatus(account.getAccountStatus())
                    .accountRole(toApiRole(roleMap.get(account.getAccountId())))
                    .build())
        .toList();
  }

  public AccountRoleUpdateResponse updateAccountRole(
      Long userId, Long accountId, AccountRoleUpdateRequest request) {

    LinkedFinancialAccount account =
        linkedFinancialAccountRepository
            .findByExternalAccountIdAndUser_UserId(accountId, userId)
            .orElseThrow(
                () -> {
                  if (linkedFinancialAccountRepository.existsByExternalAccountId(accountId)) {
                    return new BusinessException(ErrorCode.ACCOUNT_002);
                  }
                  return new BusinessException(ErrorCode.ACCOUNT_001);
                });

    AccountMapping.MappingType newType = request.getAccountRole().toMappingType();

    accountMappingRepository
        .findByLinkedFinancialAccount_LinkedAccountId(account.getLinkedAccountId())
        .ifPresentOrElse(
            existing -> existing.updateMappingType(newType),
            () ->
                accountMappingRepository.save(
                    AccountMapping.builder()
                        .userId(userId)
                        .linkedFinancialAccount(account)
                        .mappingType(newType)
                        .build()));

    return AccountRoleUpdateResponse.builder()
        .accountId(accountId)
        .accountRole(request.getAccountRole().name())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private String toApiRole(AccountMapping.MappingType type) {
    if (type == null) return null;
    return switch (type) {
      case INCOME -> "DEPOSIT";
      case SALARY -> "SALARY";
      case EMERGENCY -> "EMERGENCY";
      case STOCK -> "STOCK";
    };
  }
}
