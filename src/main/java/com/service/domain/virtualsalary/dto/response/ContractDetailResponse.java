package com.service.domain.virtualsalary.dto.response;

import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.MatchingStatus;
import com.service.domain.virtualsalary.enumtype.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractDetailResponse {

  private Long contractId;
  private String clientName;
  private BigDecimal contractAmount;
  private BigDecimal taxRate;
  private BigDecimal deductedAmount;
  private BigDecimal actualIncome;
  private TaxType taxType;
  private LocalDate expectedPaymentDate;
  private LocalDate actualPaymentDate;
  private ContractStatus contractStatus;
  private String memo;

  // 매칭 정보 (TBC 기간 전이면 null)
  private Long matchingId;
  private MatchingStatus matchingStatus;
  private Long bankTransactionId; // 입금 연결된 경우에만 존재
  private BigDecimal transactionAmount; // 실제 입금액 (Case 2/3에서 존재, 미입금이면 null)

  // TBC 상태일 때만 true → 프론트에서 완료처리 버튼 표시 여부 판단
  private boolean canComplete;
}
