package com.service.domain.mydata.service;

import com.service.domain.mydata.entity.IntegratedTransactionHistory;
import com.service.domain.mydata.entity.LinkedFinancialAccount;
import com.service.domain.mydata.repository.IntegratedTransactionHistoryRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.global.client.BankServerClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionSyncService {

  private final LinkedFinancialAccountRepository linkedFinancialAccountRepository;
  private final IntegratedTransactionHistoryRepository integratedTransactionHistoryRepository;
  private final BankServerClient bankServerClient;

  public void syncAll() {
    List<LinkedFinancialAccount> bankAccounts =
        linkedFinancialAccountRepository.findAll().stream()
            .filter(lfa -> lfa.getInstitutionType() == LinkedFinancialAccount.InstitutionType.BANK)
            .toList();

    LocalDateTime now = LocalDateTime.now();

    for (LinkedFinancialAccount lfa : bankAccounts) {
      try {
        syncAccount(lfa, now);
      } catch (Exception ignored) {
        // 개별 계좌 실패 시 나머지 계속 진행
      }
    }
  }

  private void syncAccount(LinkedFinancialAccount lfa, LocalDateTime now) {
    String fromDate =
        integratedTransactionHistoryRepository
            .findFirstByLinkedAccountIdOrderByTransactionOccurredAtDesc(lfa.getLinkedAccountId())
            .map(last -> last.getTransactionOccurredAt().toLocalDate().toString())
            .orElse(null);

    String toDate = LocalDate.now().toString();

    List<BankServerClient.TransactionItem> txList =
        bankServerClient.getTransactions(lfa.getExternalAccountId(), fromDate, toDate);

    for (BankServerClient.TransactionItem tx : txList) {
      if (integratedTransactionHistoryRepository.existsByOriginalTransactionId(
          tx.getTransactionId())) {
        continue;
      }
      integratedTransactionHistoryRepository.save(
          IntegratedTransactionHistory.builder()
              .userId(lfa.getUser().getUserId())
              .linkedAccountId(lfa.getLinkedAccountId())
              .institutionType("BANK")
              .transactionType(tx.getTransactionType())
              .transactionCategory(tx.getMerchantCategory())
              .transactionAmount(tx.getAmount())
              .balanceAfter(tx.getBalanceAfter())
              .merchantName(tx.getMerchantName())
              .originalTransactionId(tx.getTransactionId())
              .transactionOccurredAt(tx.getTransactionDateTime())
              .syncedAt(now)
              .build());
    }
  }

}
