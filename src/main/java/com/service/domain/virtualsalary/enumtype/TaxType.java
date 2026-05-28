package com.service.domain.virtualsalary.enumtype;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaxType {
  BUSINESS(new BigDecimal("0.033")),
  ETC(new BigDecimal("0.033")),
  ARTIST(new BigDecimal("0.033"));

  private final BigDecimal rate;
}
