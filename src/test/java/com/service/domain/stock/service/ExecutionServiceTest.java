package com.service.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.stock.dto.response.ExecutionListResponse;
import com.service.global.client.TransactionServerClient;
import com.service.global.client.TransactionServerClient.ExecutionItem;
import com.service.global.client.TransactionServerClient.TxPageData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

  @InjectMocks private ExecutionService executionService;

  @Mock private TransactionServerClient transactionServerClient;

  @Test
  @DisplayName("체결 내역 조회 시 TransactionServerClient에 위임하고 응답을 변환하여 반환한다")
  void getExecutions_delegatesToClientAndReturnsResponse() {
    Long accountId = 1L;
    String stockCode = "005930";
    String from = "2026-01-01";
    String to = "2026-06-13";
    int page = 0;
    int size = 10;

    TxPageData<ExecutionItem> pageData = new TxPageData<>();
    ReflectionTestUtils.setField(pageData, "content", List.of());
    ReflectionTestUtils.setField(pageData, "page", page);
    ReflectionTestUtils.setField(pageData, "size", size);
    ReflectionTestUtils.setField(pageData, "totalElements", 0L);
    ReflectionTestUtils.setField(pageData, "totalPages", 0);

    given(transactionServerClient.getExecutions(accountId, stockCode, from, to, page, size))
        .willReturn(pageData);

    ExecutionListResponse response =
        executionService.getExecutions(accountId, stockCode, from, to, page, size);

    assertThat(response).isNotNull();
    assertThat(response.getPage()).isEqualTo(page);
    assertThat(response.getSize()).isEqualTo(size);
    assertThat(response.getTotalElements()).isZero();
    then(transactionServerClient).should().getExecutions(accountId, stockCode, from, to, page, size);
  }
}
