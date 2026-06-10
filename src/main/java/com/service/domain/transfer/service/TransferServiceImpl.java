package com.service.domain.transfer.service;

import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.domain.transfer.dto.request.TransferRequest;
import com.service.domain.transfer.dto.response.TransferApproveResponse;
import com.service.domain.transfer.dto.response.TransferResponse;
import com.service.domain.transfer.dto.response.TransferResultResponse;
import com.service.global.client.TransactionServerClient;
import com.service.global.client.TransactionServerClient.BankTransferApproveResult;
import com.service.global.client.TransactionServerClient.BankTransferDetailResult;
import com.service.global.client.TransactionServerClient.BankTransferResult;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

  private final TransactionServerClient transactionServerClient;
  private final LinkedFinancialAccountRepository linkedFinancialAccountRepository;
  private final PinAuthRepository pinAuthRepository;

  @Override
  public TransferResponse requestTransfer(
      Long userId, String idempotencyKey, TransferRequest request) {

    pinAuthRepository
        .findByUserId(userId)
        .ifPresent(
            pinAuth -> {
              if (Boolean.TRUE.equals(pinAuth.getLockedYn())) {
                throw new BusinessException(ErrorCode.AUTH_009);
              }
            });

    linkedFinancialAccountRepository
        .findByExternalAccountIdAndUser_UserId(request.getFromAccountId(), userId)
        .orElseThrow(
            () -> {
              if (linkedFinancialAccountRepository.existsByExternalAccountId(
                  request.getFromAccountId())) {
                return new BusinessException(ErrorCode.ACCOUNT_002);
              }
              return new BusinessException(ErrorCode.ACCOUNT_001);
            });

    BankTransferResult result =
        transactionServerClient.bankTransfer(
            idempotencyKey,
            request.getFromAccountId(),
            request.getToBankCode(),
            request.getToAccountNumber(),
            request.getTransferAmount(),
            "USER");

    return TransferResponse.builder()
        .transferId(result.getTransferId())
        .transferStatus(result.getTransferStatus())
        .requestedAt(result.getRequestedAt())
        .build();
  }

  @Override
  public TransferApproveResponse approveTransfer(Long userId, Long transferId) {
    BankTransferDetailResult detail = transactionServerClient.getTransferResult(transferId);

    linkedFinancialAccountRepository
        .findByExternalAccountIdAndUser_UserId(detail.getFromAccountId(), userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_004));

    BankTransferApproveResult result = transactionServerClient.approveTransfer(transferId);

    return TransferApproveResponse.builder()
        .transferId(result.getTransferId())
        .transferStatus(result.getTransferStatus())
        .completedAt(result.getCompletedAt())
        .build();
  }

  @Override
  public TransferResultResponse getTransferResult(Long userId, Long transferId) {
    BankTransferDetailResult result = transactionServerClient.getTransferResult(transferId);

    linkedFinancialAccountRepository
        .findByExternalAccountIdAndUser_UserId(result.getFromAccountId(), userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_004));

    return TransferResultResponse.builder()
        .transferId(result.getTransferId())
        .fromAccountId(result.getFromAccountId())
        .toBankCode(result.getToBankCode())
        .toAccountNumber(result.getToAccountNumber())
        .transferAmount(result.getTransferAmount())
        .transferStatus(result.getTransferStatus())
        .failureReason(result.getFailureReason())
        .requestedAt(result.getRequestedAt())
        .completedAt(result.getCompletedAt())
        .build();
  }
}
