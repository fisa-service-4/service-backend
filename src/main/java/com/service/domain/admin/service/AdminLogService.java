package com.service.domain.admin.service;

import com.service.domain.admin.dto.request.ErrorLogResolveRequest;
import com.service.domain.admin.dto.response.AdminStockOrderLogResponse;
import com.service.domain.admin.dto.response.AdminTransferLogResponse;
import com.service.domain.admin.dto.response.AiLogResponse;
import com.service.domain.admin.dto.response.ApiLogResponse;
import com.service.domain.admin.dto.response.DashboardResponse;
import com.service.domain.admin.dto.response.ErrorLogResponse;
import com.service.domain.admin.dto.response.LoginLogResponse;
import com.service.domain.admin.entity.AiUsageLog;
import com.service.domain.admin.entity.ApiCallLog;
import com.service.domain.admin.entity.LoginHistory;
import com.service.domain.admin.entity.SystemErrorLog;
import com.service.domain.admin.repository.AiUsageLogRepository;
import com.service.domain.admin.repository.ApiCallLogRepository;
import com.service.domain.admin.repository.LoginHistoryRepository;
import com.service.domain.admin.repository.SystemErrorLogRepository;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.mydata.entity.IntegratedStockTransactionHistory;
import com.service.domain.mydata.repository.IntegratedStockTransactionHistoryRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.BankAdminClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLogService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String REFRESH_TOKEN_PATTERN = "refresh:*";

  private final LoginHistoryRepository loginHistoryRepository;
  private final AiUsageLogRepository aiUsageLogRepository;
  private final SystemErrorLogRepository systemErrorLogRepository;
  private final ApiCallLogRepository apiCallLogRepository;
  private final UserRepository userRepository;
  private final PinAuthRepository pinAuthRepository;
  private final StringRedisTemplate redisTemplate;
  private final BankAdminClient bankAdminClient;
  private final IntegratedStockTransactionHistoryRepository stockTransactionHistoryRepository;
  private final LinkedFinancialAccountRepository linkedFinancialAccountRepository;

  @Transactional(readOnly = true)
  public Page<LoginLogResponse> getLoginLogs(
      Long userId, String loginType, String startDate, String endDate, Pageable pageable) {
    LocalDateTime start = parseStart(startDate);
    LocalDateTime end = parseEnd(endDate);

    Specification<LoginHistory> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (userId != null) predicates.add(cb.equal(root.get("userId"), userId));
          if (loginType != null && !loginType.isBlank())
            predicates.add(cb.equal(root.get("loginType"), loginType));
          if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("loggedAt"), start));
          if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("loggedAt"), end));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return loginHistoryRepository
        .findAll(spec, pageable)
        .map(log -> LoginLogResponse.of(log, resolveUserName(log.getUserId())));
  }

  @Transactional(readOnly = true)
  public Page<AiLogResponse> getAiLogs(
      Long userId, String startDate, String endDate, Pageable pageable) {
    LocalDateTime start = parseStart(startDate);
    LocalDateTime end = parseEnd(endDate);

    Specification<AiUsageLog> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (userId != null) predicates.add(cb.equal(root.get("userId"), userId));
          if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
          if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return aiUsageLogRepository
        .findAll(spec, pageable)
        .map(
            log -> {
              String userName = log.getUserId() != null ? resolveUserName(log.getUserId()) : null;
              return AiLogResponse.of(log, userName);
            });
  }

  @Transactional(readOnly = true)
  public Page<ErrorLogResponse> getErrorLogs(
      String errorLevel, Boolean resolvedYn, String startDate, String endDate, Pageable pageable) {
    LocalDateTime start = parseStart(startDate);
    LocalDateTime end = parseEnd(endDate);

    Specification<SystemErrorLog> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (errorLevel != null && !errorLevel.isBlank())
            predicates.add(cb.equal(root.get("errorLevel"), errorLevel));
          if (resolvedYn != null) predicates.add(cb.equal(root.get("resolvedYn"), resolvedYn));
          if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
          if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return systemErrorLogRepository.findAll(spec, pageable).map(ErrorLogResponse::of);
  }

  @Transactional
  public ErrorLogResponse resolveErrorLog(Long id, ErrorLogResolveRequest request) {
    SystemErrorLog log =
        systemErrorLogRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_001));
    log.resolve(request.getResolvedMemo());
    return ErrorLogResponse.of(log);
  }

  @Transactional(readOnly = true)
  public Page<ApiLogResponse> getApiLogs(
      String serviceName, String startDate, String endDate, Pageable pageable) {
    LocalDateTime start = parseStart(startDate);
    LocalDateTime end = parseEnd(endDate);

    Specification<ApiCallLog> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (serviceName != null && !serviceName.isBlank())
            predicates.add(cb.equal(root.get("serviceName"), serviceName));
          if (start != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), start));
          if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("requestedAt"), end));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return apiCallLogRepository.findAll(spec, pageable).map(ApiLogResponse::of);
  }

  @Transactional(readOnly = true)
  public Page<AdminTransferLogResponse> getTransferHistory(
      String startDate, String endDate, Pageable pageable) {
    return bankAdminClient
        .getTransferHistory(startDate, endDate, pageable)
        .map(
            transfer -> {
              String senderName = resolveUserName(transfer.getFromUserId());
              String receiverName =
                  transfer.getToUserId() != null ? resolveUserName(transfer.getToUserId()) : null;
              return AdminTransferLogResponse.of(transfer, senderName, receiverName);
            });
  }

  @Transactional(readOnly = true)
  public Page<AdminStockOrderLogResponse> getStockOrderHistory(
      String startDate, String endDate, Pageable pageable) {
    LocalDateTime start = parseStart(startDate);
    LocalDateTime end = parseEnd(endDate);

    Specification<IntegratedStockTransactionHistory> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (start != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("transactionOccurredAt"), start));
          if (end != null)
            predicates.add(cb.lessThanOrEqualTo(root.get("transactionOccurredAt"), end));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return stockTransactionHistoryRepository
        .findAll(spec, pageable)
        .map(
            history -> {
              String buyerName = resolveUserName(history.getUserId());
              String accountMasking =
                  linkedFinancialAccountRepository
                      .findById(history.getLinkedAccountId())
                      .map(a -> a.getAccountMasking())
                      .orElse("-");
              return AdminStockOrderLogResponse.of(history, buyerName, accountMasking);
            });
  }

  @Transactional(readOnly = true)
  public DashboardResponse getDashboard() {
    LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
    LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

    long todayAiRequests = aiUsageLogRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    long todayApiCalls = apiCallLogRepository.countByRequestedAtBetween(startOfDay, endOfDay);
    long todayErrors = systemErrorLogRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    long activeSessionCount = countActiveSessionsFromRedis();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oneMinuteAgo = now.minusMinutes(1);
    Double avgMs = apiCallLogRepository.avgDurationMsByRequestedAtBetween(oneMinuteAgo, now);
    Long avgApiResponseMs = avgMs != null ? Math.round(avgMs) : null;
    long suspendedUserCount = pinAuthRepository.countByLockedYnTrue();
    long todayNewUserCount = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);

    return DashboardResponse.of(
        todayAiRequests,
        todayApiCalls,
        todayErrors,
        activeSessionCount,
        avgApiResponseMs,
        suspendedUserCount,
        todayNewUserCount);
  }

  private long countActiveSessionsFromRedis() {
    long count = 0;
    ScanOptions options = ScanOptions.scanOptions().match(REFRESH_TOKEN_PATTERN).count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext()) {
        cursor.next();
        count++;
      }
    }
    return count;
  }

  private String resolveUserName(Long userId) {
    return userRepository.findById(userId).map(u -> u.getUserName()).orElse("Unknown");
  }

  private LocalDateTime parseStart(String date) {
    if (date == null) return null;
    return LocalDate.parse(date, DATE_FORMAT).atStartOfDay();
  }

  private LocalDateTime parseEnd(String date) {
    if (date == null) return null;
    return LocalDate.parse(date, DATE_FORMAT).atTime(LocalTime.MAX);
  }
}
