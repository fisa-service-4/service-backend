package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.ManualMatchingRequest;
import com.service.domain.virtualsalary.dto.response.ManualMatchingResponse;
import com.service.domain.virtualsalary.dto.response.PaymentMatchingResponse;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import java.time.LocalDate;
import java.util.List;

public interface PaymentMatchingService {

  List<PaymentMatchingResponse> getMatchings(
      Long userId, Long contractId, MatchingStatus matchingStatus, LocalDate from, LocalDate to);

  ManualMatchingResponse manualMatch(Long userId, Long matchingId, ManualMatchingRequest request);
}
