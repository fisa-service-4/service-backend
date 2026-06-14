package com.service.domain.mydata.service;

import com.service.domain.account.service.AccountService;
import com.service.domain.mydata.dto.response.MydataConnectionResponse;
import com.service.domain.user.entity.User;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.BankServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MydataService {

  private final BankServerClient bankServerClient;
  private final UserRepository userRepository;
  private final AccountService accountService;

  public void connectAll(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    bankServerClient.connectProvider(user.getFirebaseUid(), "SHINHAN_BANK");
  }

  public MydataConnectionResponse getConnections(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    BankServerClient.ConnectionsData data = bankServerClient.getConnections(user.getFirebaseUid());
    accountService.syncForConnections(user, data);
    return MydataConnectionResponse.builder()
        .bankAccounts(data.getBankAccounts())
        .stockAccounts(data.getStockAccounts())
        .build();
  }
}
