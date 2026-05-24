package com.service.domain.admin.dto.response;

import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 사용자 목록 응답")
public class AdminUserListResponse {

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "이름", example = "홍길동")
  private String name;

  @Schema(description = "이메일", example = "user@test.com")
  private String email;

  @Schema(description = "직업 유형", example = "DEVELOPER")
  private String jobType;

  @Schema(description = "프리랜서 여부", example = "true")
  private Boolean freelancerYn;

  @Schema(description = "사용자 상태", example = "ACTIVE")
  private String status;

  @Schema(description = "가입일", example = "2026-05-01T10:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "최근 로그인 시각", example = "2026-05-17T10:00:00")
  private LocalDateTime lastLoginAt;

  public static AdminUserListResponse of(User user, LocalDateTime lastLoginAt) {
    UserProfile profile = user.getProfile();
    return AdminUserListResponse.builder()
        .userId(user.getUserId())
        .name(user.getUserName())
        .email(user.getEmail())
        .jobType(profile != null ? profile.getJobType() : null)
        .freelancerYn(profile != null ? profile.getFreelancerYn() : null)
        .status(user.getStatus().name())
        .createdAt(user.getCreatedAt())
        .lastLoginAt(lastLoginAt)
        .build();
  }
}
