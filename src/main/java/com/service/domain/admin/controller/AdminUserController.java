package com.service.domain.admin.controller;

import com.service.domain.admin.dto.request.UserStatusUpdateRequest;
import com.service.domain.admin.dto.response.AdminUserDetailResponse;
import com.service.domain.admin.dto.response.AdminUserListResponse;
import com.service.domain.admin.service.AdminUserService;
import com.service.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - User", description = "관리자 사용자 관리 API")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final AdminUserService adminUserService;

  @Operation(summary = "사용자 목록 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요")
  })
  @GetMapping
  public ResponseEntity<com.service.global.response.ApiResponse<Page<AdminUserListResponse>>>
      getUsers(
          @RequestParam(required = false) String keyword,
          @RequestParam(required = false) User.Status status,
          @RequestParam(required = false) String jobType,
          @RequestParam(required = false) String loginStatus,
          @RequestParam(required = false) String sort,
          @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            adminUserService.getUsers(keyword, status, jobType, loginStatus, sort, pageable)));
  }

  @Operation(summary = "사용자 상세 조회")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요"),
    @ApiResponse(responseCode = "404", description = "USER_001: 사용자를 찾을 수 없음")
  })
  @GetMapping("/{id}")
  public ResponseEntity<com.service.global.response.ApiResponse<AdminUserDetailResponse>>
      getUserDetail(@PathVariable Long id) {
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(adminUserService.getUserDetail(id)));
  }

  @Operation(summary = "사용자 상태 변경")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
    @ApiResponse(responseCode = "403", description = "ADMIN_001: 관리자 권한 필요"),
    @ApiResponse(responseCode = "404", description = "USER_001: 사용자를 찾을 수 없음")
  })
  @PatchMapping("/{id}/status")
  public ResponseEntity<com.service.global.response.ApiResponse<Map<String, Object>>>
      updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
    adminUserService.updateUserStatus(id, request);
    return ResponseEntity.ok(
        com.service.global.response.ApiResponse.success(
            Map.of("userId", id, "status", request.getStatus().name())));
  }
}
