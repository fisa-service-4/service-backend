package com.service.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.service.domain.admin.dto.response.DashboardResponse;
import com.service.domain.admin.repository.AiUsageLogRepository;
import com.service.domain.admin.repository.ApiCallLogRepository;
import com.service.domain.admin.repository.LoginHistoryRepository;
import com.service.domain.admin.repository.SystemErrorLogRepository;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.mydata.repository.IntegratedStockTransactionHistoryRepository;
import com.service.domain.mydata.repository.LinkedFinancialAccountRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.BankAdminClient;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class AdminLogServiceTest {

  @Mock private LoginHistoryRepository loginHistoryRepository;
  @Mock private AiUsageLogRepository aiUsageLogRepository;
  @Mock private SystemErrorLogRepository systemErrorLogRepository;
  @Mock private ApiCallLogRepository apiCallLogRepository;
  @Mock private UserRepository userRepository;
  @Mock private PinAuthRepository pinAuthRepository;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private BankAdminClient bankAdminClient;
  @Mock private IntegratedStockTransactionHistoryRepository stockTransactionHistoryRepository;
  @Mock private LinkedFinancialAccountRepository linkedFinancialAccountRepository;

  @Mock private Cursor<String> cursor;

  @InjectMocks private AdminLogService adminLogService;

  private void givenEmptyCursor() {
    given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
    given(cursor.hasNext()).willReturn(false);
  }

  // ────────────────────────────────────────────────────────────────────────────
  // L-01. getDashboard — avgApiResponseMs NULL
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("L-01: getDashboard — avgDurationMs=null → avgApiResponseMs=null")
  void getDashboard_avgApiResponseMs_null() {
    givenEmptyCursor();
    System.out.println("\n=== L-01: getDashboard avgApiResponseMs NULL ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] Redis scan → 빈 커서 (activeSessionCount=0)");
    givenEmptyCursor();

    System.out.println("[Given] apiCallLogRepository.avgDurationMsByRequestedAtBetween() = null");
    given(
            apiCallLogRepository.avgDurationMsByRequestedAtBetween(
                any(LocalDateTime.class), any(LocalDateTime.class)))
        .willReturn(null);

    DashboardResponse response = adminLogService.getDashboard();

    assertThat(response.getAvgApiResponseMs()).isNull();
    // aiUsageLogRepository, systemErrorLogRepository, pinAuthRepository, userRepository 등
    // countBy* 메서드는 mock 기본값 0L 반환 — 별도 stub 불필요

    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] adminLogService.getDashboard() 호출");
    DashboardResponse response = adminLogService.getDashboard();

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] avgApiResponseMs=null 확인");
    assertThat(response.getAvgApiResponseMs()).isNull();
    System.out.println("        ✓ avgApiResponseMs = null (avgMs == null → Math.round 미호출)");

    System.out.println("[Then] 나머지 카운트 필드 = 0 확인 (mock 기본값)");
    assertThat(response.getTodayAiRequests()).isZero();
    assertThat(response.getTodayApiCalls()).isZero();
    assertThat(response.getTodayErrors()).isZero();
    assertThat(response.getActiveSessionCount()).isZero();
    assertThat(response.getSuspendedUserCount()).isZero();
    assertThat(response.getTodayNewUserCount()).isZero();
    System.out.println("        ✓ 나머지 필드 모두 0 (외부 I/O 없는 빈 mock 환경)");

    System.out.println("\n=== PASSED ===\n");
  }

  // ────────────────────────────────────────────────────────────────────────────
  // L-02. getDashboard — avgApiResponseMs 반올림
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @DisplayName("L-02: getDashboard — avgDurationMs=123.7 → avgApiResponseMs=124 (Math.round)")
  void getDashboard_avgApiResponseMs_rounded() {
    givenEmptyCursor();
    System.out.println("\n=== L-02: getDashboard avgApiResponseMs 반올림 ===");

    // ── Given ──────────────────────────────────────────────────
    System.out.println("[Given] Redis scan → 빈 커서");
    givenEmptyCursor();

    System.out.println("[Given] apiCallLogRepository.avgDurationMsByRequestedAtBetween() = 123.7");
    given(
            apiCallLogRepository.avgDurationMsByRequestedAtBetween(
                any(LocalDateTime.class), any(LocalDateTime.class)))
        .willReturn(123.7);

    DashboardResponse response = adminLogService.getDashboard();

    assertThat(response.getAvgApiResponseMs()).isEqualTo(124L);
    // ── When ───────────────────────────────────────────────────
    System.out.println("\n[When] adminLogService.getDashboard() 호출");
    DashboardResponse response = adminLogService.getDashboard();

    // ── Then ───────────────────────────────────────────────────
    System.out.println("\n[Then] avgApiResponseMs=124 확인");
    assertThat(response.getAvgApiResponseMs()).isEqualTo(124L);
    System.out.println("        ✓ avgApiResponseMs = 124 (Math.round(123.7) = 124)");

    System.out.println("\n=== PASSED ===\n");
  }
}
