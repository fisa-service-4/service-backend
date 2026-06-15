package com.service.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.service.domain.admin.dto.response.AdminUserListResponse;
import com.service.domain.admin.service.AdminLogSaveService;
import com.service.domain.admin.service.AdminUserService;
import com.service.domain.user.entity.User;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import com.service.global.security.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminUserService adminUserService;
  @MockBean private JwtProvider jwtProvider;
  @MockBean private AdminLogSaveService adminLogSaveService;

  private AdminUserListResponse buildListResponse(Long userId, String status) {
    return AdminUserListResponse.builder()
        .userId(userId)
        .name("홍길동")
        .email("user@test.com")
        .status(status)
        .isOnline(false)
        .build();
  }

  // ────────────────────────────────────────────────────────────────────────────
  // M-01. PATCH /admin/users/{id}/status — ACTIVE → HTTP 200
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("M-01: status=ACTIVE → HTTP 200, userId·status 응답 확인")
  void updateUserStatus_active_returnsOkWithPayload() throws Exception {
    willDoNothing().given(adminUserService).updateUserStatus(eq(1L), any());

    mockMvc
        .perform(
            patch("/api/v1/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

    then(adminUserService).should().updateUserStatus(eq(1L), any());
  }

  // ────────────────────────────────────────────────────────────────────────────
  // M-02. PATCH /admin/users/{id}/status — LOCKED → HTTP 200
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("M-02: status=LOCKED → HTTP 200, pinAuth.unlock() 미호출 (서비스 책임)")
  void updateUserStatus_locked_returnsOk() throws Exception {
    willDoNothing().given(adminUserService).updateUserStatus(eq(1L), any());

    mockMvc
        .perform(
            patch("/api/v1/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LOCKED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.status").value("LOCKED"));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // M-03. PATCH /admin/users/{id}/status — User 없음 → HTTP 404 (USER_001)
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("M-03: 존재하지 않는 userId → BusinessException(USER_001) → HTTP 404")
  void updateUserStatus_userNotFound_returns404() throws Exception {
    willThrow(new BusinessException(ErrorCode.USER_001))
        .given(adminUserService)
        .updateUserStatus(eq(99L), any());

    mockMvc
        .perform(
            patch("/api/v1/admin/users/99/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LOCKED\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("USER_001"));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // N-01. GET /admin/users?loginStatus=ONLINE → HTTP 200
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("N-01: loginStatus=ONLINE → 서비스 호출 시 ONLINE 전달, HTTP 200")
  void getUsers_loginStatusOnline_passedToService() throws Exception {
    given(
            adminUserService.getUsers(
                isNull(), isNull(), isNull(), eq("ONLINE"), isNull(), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(buildListResponse(1L, "ACTIVE"))));

    mockMvc
        .perform(get("/api/v1/admin/users").param("loginStatus", "ONLINE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].userId").value(1));

    then(adminUserService)
        .should()
        .getUsers(isNull(), isNull(), isNull(), eq("ONLINE"), isNull(), any(Pageable.class));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // N-02. GET /admin/users?loginStatus=OFFLINE → HTTP 200
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("N-02: loginStatus=OFFLINE → 서비스 호출 시 OFFLINE 전달, HTTP 200")
  void getUsers_loginStatusOffline_passedToService() throws Exception {
    given(
            adminUserService.getUsers(
                isNull(), isNull(), isNull(), eq("OFFLINE"), isNull(), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(buildListResponse(2L, "ACTIVE"))));

    mockMvc
        .perform(get("/api/v1/admin/users").param("loginStatus", "OFFLINE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].userId").value(2));

    then(adminUserService)
        .should()
        .getUsers(isNull(), isNull(), isNull(), eq("OFFLINE"), isNull(), any(Pageable.class));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // N-03. GET /admin/users?status=ACTIVE → HTTP 200
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("N-03: status=ACTIVE → User.Status.ACTIVE 변환 후 서비스 전달, HTTP 200")
  void getUsers_statusActive_convertedAndPassed() throws Exception {
    given(
            adminUserService.getUsers(
                isNull(),
                eq(User.Status.ACTIVE),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(buildListResponse(1L, "ACTIVE"))));

    mockMvc
        .perform(get("/api/v1/admin/users").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));

    then(adminUserService)
        .should()
        .getUsers(
            isNull(), eq(User.Status.ACTIVE), isNull(), isNull(), isNull(), any(Pageable.class));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // N-04. GET /admin/users?sort=name → sort 파라미터 서비스 전달
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("N-04: sort=name → 서비스에 sort=\"name\" 전달, HTTP 200")
  void getUsers_sortName_passedToService() throws Exception {
    given(
            adminUserService.getUsers(
                isNull(), isNull(), isNull(), isNull(), eq("name"), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    mockMvc
        .perform(get("/api/v1/admin/users").param("sort", "name"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());

    then(adminUserService)
        .should()
        .getUsers(isNull(), isNull(), isNull(), isNull(), eq("name"), any(Pageable.class));
  }

  // ────────────────────────────────────────────────────────────────────────────
  // N-05. GET /admin/users?keyword=홍길동 → keyword 파라미터 서비스 전달
  // ────────────────────────────────────────────────────────────────────────────
  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("N-05: keyword=홍길동 → 서비스에 keyword 전달, 검색 결과 반환")
  void getUsers_withKeyword_passedToService() throws Exception {
    given(
            adminUserService.getUsers(
                eq("홍길동"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(buildListResponse(1L, "ACTIVE"))));

    mockMvc
        .perform(get("/api/v1/admin/users").param("keyword", "홍길동"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
        .andExpect(jsonPath("$.data.content[0].userId").value(1));

    then(adminUserService)
        .should()
        .getUsers(eq("홍길동"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
  }
}
