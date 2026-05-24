package com.service.domain.admin.dto.response;

import com.service.domain.admin.entity.LoginHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 로그 응답")
public class LoginLogResponse {

  @Schema(description = "로그인 이력 ID", example = "1")
  private Long loginHistoryId;

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "사용자명", example = "홍길동")
  private String userName;

  @Schema(description = "로그인 유형", example = "LOGIN")
  private String loginType;

  @Schema(description = "IP 주소", example = "127.0.0.1")
  private String ipAddress;

  @Schema(description = "디바이스 정보", example = "iPhone / iOS 17")
  private String device;

  @Schema(description = "실패 사유")
  private String failReason;

  @Schema(description = "로그인 시각", example = "2026-05-17T10:00:00")
  private LocalDateTime loggedAt;

  public static LoginLogResponse of(LoginHistory history, String userName) {
    return LoginLogResponse.builder()
        .loginHistoryId(history.getLoginHistoryId())
        .userId(history.getUserId())
        .userName(userName)
        .loginType(history.getLoginType())
        .ipAddress(history.getIpAddress())
        .device(history.getDeviceInfo())
        .failReason(history.getFailReason())
        .loggedAt(history.getLoggedAt())
        .build();
  }
}
