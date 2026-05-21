package com.service.domain.user.dto.response;

import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

  private Long userId;
  private String email;
  private String userName;
  private String phoneNumber;
  private String role;
  private String status;
  private Boolean notificationConsentYn;
  private Boolean mydataConsentYn;
  private Boolean freelancerYn;
  private String jobType;
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
