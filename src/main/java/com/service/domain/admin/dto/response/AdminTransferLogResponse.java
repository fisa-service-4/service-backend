package com.service.domain.admin.dto.response;

import com.service.global.client.BankAdminClient.TransferData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminTransferLogResponse {

  private String senderName;
  private String fromAccountNumberMasked;
  private BigDecimal transferAmount;
  private String receiverName;
  private String toAccountNumberMasked;
  private LocalDateTime transferredAt;

  public static AdminTransferLogResponse of(
      TransferData transfer, String senderName, String receiverName) {
    return AdminTransferLogResponse.builder()
        .senderName(senderName)
        .fromAccountNumberMasked(mask(transfer.getFromAccountNumber()))
        .transferAmount(transfer.getTransferAmount())
        .receiverName(receiverName)
        .toAccountNumberMasked(mask(transfer.getToAccountNumber()))
        .transferredAt(transfer.getRequestedAt())
        .build();
  }

  private static String mask(String accountNumber) {
    if (accountNumber == null) return null;
    return accountNumber.replaceAll("^([^-]+)-([^-]+)-(.+)$", "$1-***-$3");
  }
}
