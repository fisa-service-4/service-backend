package com.service.domain.admin.dto.response;

import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 사용자 상세 응답")
public class AdminUserDetailResponse {

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "이름", example = "홍길동")
  private String name;

  @Schema(description = "이메일", example = "user@test.com")
  private String email;

  @Schema(description = "전화번호 (마스킹)", example = "010****1234")
  private String phoneNumber;

  @Schema(description = "직업 유형", example = "DEVELOPER")
  private String jobType;

  @Schema(description = "프리랜서 여부", example = "true")
  private Boolean freelancerYn;

  @Schema(description = "사용자 상태", example = "ACTIVE")
  private String status;

  @Schema(description = "약관 동의 여부", example = "true")
  private Boolean termsConsentYn;

  @Schema(description = "마이데이터 동의 여부", example = "true")
  private Boolean mydataConsentYn;

  @Schema(description = "알림 동의 여부", example = "true")
  private Boolean notificationConsentYn;

  @Schema(description = "가입일", example = "2026-05-01T10:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "최근 로그인 시각", example = "2026-05-17T10:00:00")
  private LocalDateTime lastLoginAt;

  @Schema(description = "온라인 여부", example = "true")
  private Boolean isOnline;

  public static AdminUserDetailResponse of(User user, LocalDateTime lastLoginAt, Boolean isOnline) {
    UserProfile profile = user.getProfile();
    return AdminUserDetailResponse.builder()
        .userId(user.getUserId())
        .name(user.getUserName())
        .email(user.getEmail())
        .phoneNumber(maskPhoneNumber(user.getPhoneNumber()))
        .jobType(profile != null ? profile.getJobType() : null)
        .freelancerYn(profile != null ? profile.getFreelancerYn() : null)
        .status(user.getStatus().name())
        .termsConsentYn(user.getTermsConsentYn())
        .mydataConsentYn(user.getMydataConsentYn())
        .notificationConsentYn(user.getNotificationConsentYn())
        .createdAt(user.getCreatedAt())
        .lastLoginAt(lastLoginAt)
        .isOnline(isOnline)
        .build();
  }

  private static String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.length() < 8) {
      return phoneNumber;
    }
    return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
  }
}
