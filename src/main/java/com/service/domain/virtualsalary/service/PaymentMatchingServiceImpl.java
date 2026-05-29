package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.ManualMatchingRequest;
import com.service.domain.virtualsalary.dto.response.ManualMatchingResponse;
import com.service.domain.virtualsalary.dto.response.PaymentMatchingResponse;
import com.service.domain.virtualsalary.entity.PaymentMatching;
import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.MatchedBy;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.repository.PaymentMatchingRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentMatchingServiceImpl implements PaymentMatchingService {

  private final PaymentMatchingRepository paymentMatchingRepository;
  private final AutoDistributionService autoDistributionService;

  @Override
  @Transactional(readOnly = true)
  public List<PaymentMatchingResponse> getMatchings(
      Long userId, Long contractId, MatchingStatus matchingStatus, LocalDate from, LocalDate to) {

    LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
    LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

    return paymentMatchingRepository
        .findAllByFilters(userId, contractId, matchingStatus, fromDateTime, toDateTime)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public ManualMatchingResponse manualMatch(
      Long userId, Long matchingId, ManualMatchingRequest request) {

    if (!MatchedBy.USER.name().equals(request.getMatchedBy())) {
      throw new BusinessException(ErrorCode.MATCHING_003);
    }

    PaymentMatching matching =
        paymentMatchingRepository
            .findByMatchingIdAndContract_UserId(matchingId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MATCHING_001));

    if (matching.getMatchingStatus() == MatchingStatus.MATCHED
        || matching.getMatchingStatus() == MatchingStatus.MANUAL_MATCHED) {
      throw new BusinessException(ErrorCode.MATCHING_002);
    }

    matching.applyManualMatch(request.getBankTransactionId());
    matching.getContract().updateContractStatus(ContractStatus.PAID);

    autoDistributionService.distribute(userId, matching.getMatchingId());

    return ManualMatchingResponse.builder()
        .matchingId(matching.getMatchingId())
        .matchingStatus(matching.getMatchingStatus())
        .matchedBy(matching.getMatchedBy())
        .matchedAt(matching.getMatchedAt())
        .build();
  }

  private PaymentMatchingResponse toResponse(PaymentMatching pm) {
    return PaymentMatchingResponse.builder()
        .matchingId(pm.getMatchingId())
        .contractId(pm.getContract().getContractId())
        .bankTransactionId(pm.getBankTransactionId())
        .matchingStatus(pm.getMatchingStatus())
        .matchedBy(pm.getMatchedBy())
        .matchedAt(pm.getMatchedAt())
        .build();
  }
}
