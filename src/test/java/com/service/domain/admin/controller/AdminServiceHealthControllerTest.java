package com.service.domain.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.service.domain.admin.dto.response.ServiceHealthResponse;
import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.admin.service.AdminServiceHealthService;
import com.service.global.security.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminServiceHealthController.class)
class AdminServiceHealthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminServiceHealthService adminServiceHealthService;
  @MockBean private JwtProvider jwtProvider;
  @MockBean private AdminLogSaveService adminLogSaveService;

  // ────────────────────────────────────────────────────────────────────────────
  // P-01. GET /admin/services/health — 서비스 DOWN → HTTP 200 유지
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("P-01: 일부 서비스 DOWN → HTTP 200 유지 (상태 정보는 data 필드에 포함)")
  void getServicesHealth_someDown_alwaysReturns200() throws Exception {
    List<ServiceHealthResponse> healthList =
        List.of(
            new ServiceHealthResponse("bank-server", "DOWN", "Connection refused"),
            new ServiceHealthResponse("stock-server", "UP", null),
            new ServiceHealthResponse("transaction-server", "UP", null),
            new ServiceHealthResponse("mydata-server", "UP", null),
            new ServiceHealthResponse("ai-server", "UP", null));
    given(adminServiceHealthService.getAllServicesHealth()).willReturn(healthList);

    mockMvc
        .perform(get("/api/v1/admin/services/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].serviceName").value("bank-server"))
        .andExpect(jsonPath("$.data[0].status").value("DOWN"))
        .andExpect(jsonPath("$.data[0].error").value("Connection refused"))
        .andExpect(jsonPath("$.data[1].status").value("UP"));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // P-02. GET /admin/services/health — 전체 서비스 UP → HTTP 200
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("P-02: 전체 서비스 UP → HTTP 200, 모든 status=UP 반환")
  void getServicesHealth_allUp_returns200WithAllUp() throws Exception {
    List<ServiceHealthResponse> healthList =
        List.of(
            new ServiceHealthResponse("bank-server", "UP", null),
            new ServiceHealthResponse("stock-server", "UP", null),
            new ServiceHealthResponse("transaction-server", "UP", null),
            new ServiceHealthResponse("mydata-server", "UP", null),
            new ServiceHealthResponse("ai-server", "UP", null));
    given(adminServiceHealthService.getAllServicesHealth()).willReturn(healthList);

    mockMvc
        .perform(get("/api/v1/admin/services/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(5))
        .andExpect(jsonPath("$.data[0].serviceName").value("bank-server"))
        .andExpect(jsonPath("$.data[0].status").value("UP"))
        .andExpect(jsonPath("$.data[4].serviceName").value("ai-server"))
        .andExpect(jsonPath("$.data[4].status").value("UP"));
  }
}
