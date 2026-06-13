package com.service.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.stock.dto.response.HoldingListResponse;
import com.service.domain.stock.dto.response.HoldingReturnsResponse;
import com.service.global.client.TransactionServerClient;
import com.service.global.client.TransactionServerClient.HoldingItem;
import com.service.global.client.TransactionServerClient.ReturnsItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

  @InjectMocks private HoldingService holdingService;

  @Mock private TransactionServerClient transactionServerClient;

  @Test
  @DisplayName("보유 종목 조회 시 TransactionServerClient에 위임하고 목록을 반환한다")
  void getHoldings_delegatesToClientAndReturnsList() {
    Long accountId = 1L;
    HoldingItem item = new HoldingItem();
    ReflectionTestUtils.setField(item, "stockCode", "005930");
    ReflectionTestUtils.setField(item, "stockName", "삼성전자");
    ReflectionTestUtils.setField(item, "quantity", 10);
    ReflectionTestUtils.setField(item, "averagePrice", new BigDecimal("70000"));
    ReflectionTestUtils.setField(item, "currentPrice", new BigDecimal("75000"));
    given(transactionServerClient.getHoldings(accountId)).willReturn(List.of(item));

    HoldingListResponse response = holdingService.getHoldings(accountId);

    assertThat(response.getHoldings()).hasSize(1);
    assertThat(response.getHoldings().get(0).getStockCode()).isEqualTo("005930");
    then(transactionServerClient).should().getHoldings(accountId);
  }

  @Test
  @DisplayName("보유 종목이 없을 때 빈 목록을 반환한다")
  void getHoldings_returnsEmptyListWhenNoHoldings() {
    Long accountId = 1L;
    given(transactionServerClient.getHoldings(accountId)).willReturn(List.of());

    HoldingListResponse response = holdingService.getHoldings(accountId);

    assertThat(response.getHoldings()).isEmpty();
  }

  @Test
  @DisplayName("수익률 조회 시 TransactionServerClient에 위임하고 결과를 반환한다")
  void getReturns_delegatesToClientAndReturnsResponse() {
    Long accountId = 1L;
    ReturnsItem returnsItem = new ReturnsItem();
    ReflectionTestUtils.setField(returnsItem, "dailyReturnRate", new BigDecimal("0.5"));
    ReflectionTestUtils.setField(returnsItem, "monthlyReturnRate", new BigDecimal("3.2"));
    ReflectionTestUtils.setField(returnsItem, "yearlyReturnRate", new BigDecimal("11.5"));
    given(transactionServerClient.getReturns(accountId)).willReturn(returnsItem);

    HoldingReturnsResponse response = holdingService.getReturns(accountId);

    assertThat(response.getDailyReturnRate()).isEqualByComparingTo(new BigDecimal("0.5"));
    assertThat(response.getMonthlyReturnRate()).isEqualByComparingTo(new BigDecimal("3.2"));
    assertThat(response.getYearlyReturnRate()).isEqualByComparingTo(new BigDecimal("11.5"));
    then(transactionServerClient).should().getReturns(accountId);
  }
}
