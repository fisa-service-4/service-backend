package com.service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor

// ErrorCode 정의 역할
public enum ErrorCode {

  // AUTH
  AUTH_001(HttpStatus.CONFLICT, "AUTH_001", "이미 가입된 이메일입니다."),
  AUTH_002(HttpStatus.CONFLICT, "AUTH_002", "이미 가입된 전화번호입니다."),
  AUTH_003(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다."),
  AUTH_004(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 토큰입니다."),
  AUTH_005(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 토큰입니다."),
  AUTH_006(HttpStatus.BAD_REQUEST, "AUTH_006", "인증번호가 올바르지 않습니다."),
  AUTH_007(HttpStatus.BAD_REQUEST, "AUTH_007", "인증번호가 만료되었습니다."),
  AUTH_008(HttpStatus.BAD_REQUEST, "AUTH_008", "PIN이 올바르지 않습니다."),
  AUTH_009(HttpStatus.BAD_REQUEST, "AUTH_009", "PIN이 잠겼습니다. 고객센터에 문의해주세요."),
  AUTH_010(HttpStatus.BAD_REQUEST, "AUTH_010", "연속 또는 반복 숫자는 사용할 수 없습니다."),
  AUTH_011(HttpStatus.BAD_REQUEST, "AUTH_011", "본인 확인에 실패했습니다."),

  // USER
  USER_001(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),

  // ADMIN
  ADMIN_001(HttpStatus.FORBIDDEN, "ADMIN_001", "관리자 권한이 필요합니다."),

  // CONTRACT
  CONTRACT_001(HttpStatus.NOT_FOUND, "CONTRACT_001", "존재하지 않는 계약입니다."),
  CONTRACT_002(HttpStatus.FORBIDDEN, "CONTRACT_002", "본인 계약이 아닙니다."),

  // VIRTUAL_SALARY
  VIRTUAL_SALARY_001(HttpStatus.NOT_FOUND, "VIRTUAL_SALARY_001", "가상월급 설정이 없습니다."),
  VIRTUAL_SALARY_002(
      HttpStatus.BAD_REQUEST, "VIRTUAL_SALARY_002", "투자 비율과 비상금 비율의 합은 100을 초과할 수 없습니다."),
  VIRTUAL_SALARY_003(HttpStatus.NOT_FOUND, "VIRTUAL_SALARY_003", "SALARY 계좌가 연결되어 있지 않습니다."),

  // MATCHING
  MATCHING_001(HttpStatus.NOT_FOUND, "MATCHING_001", "매칭 정보를 찾을 수 없습니다."),
  MATCHING_002(HttpStatus.BAD_REQUEST, "MATCHING_002", "이미 매칭 처리된 건입니다."),
  MATCHING_003(HttpStatus.BAD_REQUEST, "MATCHING_003", "matchedBy는 USER만 허용됩니다."),

  // VALID
  VALID_001(HttpStatus.BAD_REQUEST, "VALID_001", "입력값이 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
