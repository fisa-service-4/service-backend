package com.service.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.service.domain.admin.dto.response.ErrorLogResponse;
import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.admin.service.AdminLogService;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import com.service.global.security.JwtProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminLogController.class)
class AdminLogControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminLogService adminLogService;
  @MockBean private JwtProvider jwtProvider;
  @MockBean private AdminLogSaveService adminLogSaveService;

  // ────────────────────────────────────────────────────────────────────────────
  // O-01. PATCH /admin/logs/error/{id} — resolvedYn=true DB 반영 검증
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("O-01: resolveErrorLog — resolvedYn=true, resolvedAt 설정 후 HTTP 200 반환")
  void resolveErrorLog_success_returnsResolvedResponse() throws Exception {
    LocalDateTime resolvedAt = LocalDateTime.of(2026, 6, 15, 10, 0, 0);
    ErrorLogResponse resolvedResponse =
        ErrorLogResponse.builder()
            .errorLogId(1L)
            .serviceName("service-backend")
            .errorLevel("ERROR")
            .errorMessage("Connection timeout")
            .resolvedYn(true)
            .resolvedAt(resolvedAt)
            .createdAt(LocalDateTime.of(2026, 6, 15, 9, 0, 0))
            .build();

    given(adminLogService.resolveErrorLog(eq(1L), any())).willReturn(resolvedResponse);

    mockMvc
        .perform(
            patch("/api/v1/admin/logs/error/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resolvedYn\":true,\"resolvedMemo\":\"DB 연결 설정 수정\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.errorLogId").value(1))
        .andExpect(jsonPath("$.data.resolvedYn").value(true))
        .andExpect(jsonPath("$.data.resolvedAt").value("2026-06-15T10:00:00"));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // O-02. PATCH /admin/logs/error/{id} — 존재하지 않는 id → HTTP 403 (ADMIN_001)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("O-02: 존재하지 않는 errorLogId → BusinessException(ADMIN_001) → HTTP 403")
  void resolveErrorLog_notFound_returns403() throws Exception {
    willThrow(new BusinessException(ErrorCode.ADMIN_001))
        .given(adminLogService)
        .resolveErrorLog(eq(99L), any());

    mockMvc
        .perform(
            patch("/api/v1/admin/logs/error/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resolvedYn\":true}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("ADMIN_001"));
  }
}
