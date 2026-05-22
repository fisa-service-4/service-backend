package com.service.domain.user.dto.response;

import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내 정보 응답")
public class UserResponse {

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "이메일", example = "user@test.com")
  private String email;

  @Schema(description = "사용자 이름", example = "홍길동")
  private String userName;

  @Schema(description = "휴대폰 번호 (마스킹)", example = "010****1234")
  private String phoneNumber;

  @Schema(description = "권한", example = "USER")
  private String role;

  @Schema(description = "계정 상태", example = "ACTIVE")
  private String status;

  @Schema(description = "알림 수신 동의 여부", example = "true")
  private Boolean notificationConsentYn;

  @Schema(description = "마이데이터 동의 여부", example = "false")
  private Boolean mydataConsentYn;

  @Schema(description = "프리랜서 여부", example = "true")
  private Boolean freelancerYn;

  @Schema(description = "직업 유형", example = "DEVELOPER")
  private String jobType;

  @Schema(description = "가입일", example = "2026-05-01T10:00:00")
  private LocalDateTime createdAt;

  public static UserResponse of(User user, UserProfile profile) {
    return UserResponse.builder()
        .userId(user.getUserId())
        .email(user.getEmail())
        .userName(user.getUserName())
        .phoneNumber(user.getPhoneNumber())
        .role(user.getRole().name())
        .status(user.getStatus().name())
        .notificationConsentYn(user.getNotificationConsentYn())
        .mydataConsentYn(user.getMydataConsentYn())
        .freelancerYn(profile.getFreelancerYn())
        .jobType(profile.getJobType())
        .createdAt(user.getCreatedAt())
        .build();
  }
}
