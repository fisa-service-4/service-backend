package com.service.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.stock.dto.response.CashBalanceResponse;
import com.service.domain.stock.dto.response.StockAccountsResponse;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.client.TransactionServerClient.CashBalanceItem;
import com.service.global.client.TransactionServerClient.StockAccountItem;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

  @InjectMocks private StockService stockService;

  @Mock private TransactionServerClient transactionServerClient;

  @Mock private UserRepository userRepository;

  @Test
  @DisplayName("예수금 조회 시 TransactionServerClient에 위임하고 잔액을 반환한다")
  void getCashBalance_delegatesToClientAndReturnsBalance() {
    Long accountId = 1L;
    CashBalanceItem balanceItem = new CashBalanceItem();
    ReflectionTestUtils.setField(balanceItem, "cashBalance", new BigDecimal("500000"));
    ReflectionTestUtils.setField(balanceItem, "availableBalance", new BigDecimal("480000"));
    given(transactionServerClient.getCashBalance(accountId)).willReturn(balanceItem);

    CashBalanceResponse response = stockService.getCashBalance(accountId);

    assertThat(response).isNotNull();
    assertThat(response.getCashBalance()).isEqualByComparingTo(new BigDecimal("500000"));
    assertThat(response.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("480000"));
    then(transactionServerClient).should().getCashBalance(accountId);
  }

  @Test
  @DisplayName("증권 계좌 조회 시 유저의 firebaseUid로 계좌 목록을 반환한다")
  void getStockAccounts_returnsAccountsForUser() {
    Long userId = 1L;
    String firebaseUid = "firebase-uid-abc";
    User user = User.builder().firebaseUid(firebaseUid).build();
    StockAccountItem accountItem = new StockAccountItem();
    ReflectionTestUtils.setField(accountItem, "accountId", 100L);
    ReflectionTestUtils.setField(accountItem, "accountNumber", "123-456-789");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(transactionServerClient.getStockAccounts(firebaseUid)).willReturn(List.of(accountItem));

    StockAccountsResponse response = stockService.getStockAccounts(userId);

    assertThat(response.getAccounts()).hasSize(1);
    then(transactionServerClient).should().getStockAccounts(firebaseUid);
  }

  @Test
  @DisplayName("존재하지 않는 userId로 증권 계좌 조회 시 USER_001 예외가 발생한다")
  void getStockAccounts_throwsExceptionWhenUserNotFound() {
    Long userId = 99L;
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> stockService.getStockAccounts(userId))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_001);
  }

  @Test
  @DisplayName("증권 계좌 조회 중 BusinessException 발생 시 빈 목록을 반환한다")
  void getStockAccounts_returnsEmptyListWhenClientThrowsBusinessException() {
    Long userId = 1L;
    String firebaseUid = "firebase-uid-abc";
    User user = User.builder().firebaseUid(firebaseUid).build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(transactionServerClient.getStockAccounts(firebaseUid))
        .willThrow(new BusinessException(ErrorCode.USER_001));

    StockAccountsResponse response = stockService.getStockAccounts(userId);

    assertThat(response.getAccounts()).isEmpty();
  }
}
