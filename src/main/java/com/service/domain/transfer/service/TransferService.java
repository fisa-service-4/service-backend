package com.service.domain.transfer.service;

import com.service.domain.transfer.dto.request.TransferRequest;
import com.service.domain.transfer.dto.response.TransferResponse;
import com.service.domain.transfer.dto.response.TransferResultResponse;

public interface TransferService {

  TransferResponse requestTransfer(Long userId, String idempotencyKey, TransferRequest request);

  TransferResultResponse getTransferResult(Long userId, Long transferId);
}
