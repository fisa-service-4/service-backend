package com.service.domain.account.service;

import com.service.domain.account.dto.request.AccountRoleUpdateRequest;
import com.service.domain.account.dto.response.AccountListResponse;
import com.service.domain.account.dto.response.AccountRoleUpdateResponse;
import com.service.domain.mydata.entity.AccountMapping;
import com.service.domain.mydata.entity.LinkedFinancialAccount;
import com.service.domain.mydata.repository.AccountMappingRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserRepository;
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
  private final UserRepository userRepository;

  public List<AccountListResponse> getMyAccounts(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    BankServerClient.ConnectionsData connections =
        bankServerClient.getConnections(user.getFirebaseUid());

    List<BankServerClient.BankAccountItem> bankAccounts = connections.getBankAccounts();
    List<BankServerClient.StockAccountItem> stockAccounts = connections.getStockAccounts();

    syncLinkedAccounts(user, bankAccounts, stockAccounts);

    Map<Long, AccountMapping.MappingType> roleMap =
        accountMappingRepository.findByUserId(userId).stream()
            .collect(
                Collectors.toMap(
                    m -> m.getLinkedFinancialAccount().getExternalAccountId(),
                    AccountMapping::getMappingType,
                    (a, b) -> a));

    List<AccountListResponse> result = new java.util.ArrayList<>();
    bankAccounts.forEach(
        account ->
            result.add(
                AccountListResponse.builder()
                    .accountId(account.getAccountId())
                    .bankCode(account.getBankCode())
                    .accountNumber(account.getAccountNumber())
                    .accountName(account.getAccountName())
                    .balance(account.getBalance())
                    .accountRole(toApiRole(roleMap.get(account.getAccountId())))
                    .build()));
    stockAccounts.forEach(
        account ->
            result.add(
                AccountListResponse.builder()
                    .accountId(account.getAccountId())
                    .bankCode(account.getBankCode())
                    .accountNumber(account.getAccountNumber())
                    .accountName(account.getAccountName())
                    .balance(account.getCashBalance())
                    .accountRole(toApiRole(roleMap.get(account.getAccountId())))
                    .build()));
    return result;
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

    if (newType == AccountMapping.MappingType.STOCK
        && account.getInstitutionType() != LinkedFinancialAccount.InstitutionType.SECURITIES) {
      throw new BusinessException(ErrorCode.ACCOUNT_005);
    }

    // 같은 역할이 다른 계좌에 이미 있으면 해제
    accountMappingRepository
        .findByUserIdAndMappingType(userId, newType)
        .ifPresent(
            existing -> {
              if (!existing
                  .getLinkedFinancialAccount()
                  .getLinkedAccountId()
                  .equals(account.getLinkedAccountId())) {
                accountMappingRepository.delete(existing);
              }
            });

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

  public void syncForConnections(User user, BankServerClient.ConnectionsData data) {
    if (data == null) return;
    List<BankServerClient.BankAccountItem> bankAccounts =
        data.getBankAccounts() != null ? data.getBankAccounts() : List.of();
    List<BankServerClient.StockAccountItem> stockAccounts =
        data.getStockAccounts() != null ? data.getStockAccounts() : List.of();
    syncLinkedAccounts(user, bankAccounts, stockAccounts);
  }

  private void syncLinkedAccounts(
      User user,
      List<BankServerClient.BankAccountItem> bankAccounts,
      List<BankServerClient.StockAccountItem> stockAccounts) {
    for (BankServerClient.BankAccountItem account : bankAccounts) {
      boolean exists =
          linkedFinancialAccountRepository
              .findByExternalAccountIdAndUser_UserId(account.getAccountId(), user.getUserId())
              .isPresent();
      if (!exists) {
        linkedFinancialAccountRepository.save(
            LinkedFinancialAccount.builder()
                .user(user)
                .institutionType(LinkedFinancialAccount.InstitutionType.BANK)
                .institutionCode(account.getBankCode())
                .externalAccountId(account.getAccountId())
                .accountMasking(account.getAccountNumber())
                .syncedAt(LocalDateTime.now())
                .build());
      }
    }
    for (BankServerClient.StockAccountItem account : stockAccounts) {
      boolean exists =
          linkedFinancialAccountRepository
              .findByExternalAccountIdAndUser_UserId(account.getAccountId(), user.getUserId())
              .isPresent();
      if (!exists) {
        linkedFinancialAccountRepository.save(
            LinkedFinancialAccount.builder()
                .user(user)
                .institutionType(LinkedFinancialAccount.InstitutionType.SECURITIES)
                .institutionCode(account.getBankCode())
                .externalAccountId(account.getAccountId())
                .accountMasking(account.getAccountNumber())
                .syncedAt(LocalDateTime.now())
                .build());
      }
    }
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
