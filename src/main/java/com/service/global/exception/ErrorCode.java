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
  VIRTUAL_SALARY_003(HttpStatus.NOT_FOUND, "VIRTUAL_SALARY_003", "SALARY 계좌가 연결되어 있지 않습니다."),

  // MATCHING
  MATCHING_001(HttpStatus.NOT_FOUND, "MATCHING_001", "매칭 정보를 찾을 수 없습니다."),
  MATCHING_002(HttpStatus.BAD_REQUEST, "MATCHING_002", "이미 매칭 처리된 건입니다."),
  MATCHING_003(HttpStatus.BAD_REQUEST, "MATCHING_003", "matchedBy는 USER만 허용됩니다."),

  // AI
  AI_001(HttpStatus.INTERNAL_SERVER_ERROR, "AI_001", "AI 응답 생성에 실패했습니다."),
  AI_002(HttpStatus.GATEWAY_TIMEOUT, "AI_002", "AI 서버 응답 시간이 초과되었습니다."),
  AI_003(HttpStatus.INTERNAL_SERVER_ERROR, "AI_003", "AI 실행에 실패했습니다."),

  // VALID
  VALID_001(HttpStatus.BAD_REQUEST, "VALID_001", "입력값이 올바르지 않습니다."),

  // ACCOUNT
  ACCOUNT_001(HttpStatus.NOT_FOUND, "ACCOUNT_001", "해당 계좌를 찾을 수 없습니다."),
  ACCOUNT_002(HttpStatus.FORBIDDEN, "ACCOUNT_002", "본인 계좌가 아닙니다."),
  ACCOUNT_003(HttpStatus.BAD_REQUEST, "ACCOUNT_003", "계좌 상태가 유효하지 않습니다."),
  ACCOUNT_004(HttpStatus.CONFLICT, "ACCOUNT_004", "해당 역할은 이미 다른 계좌에 설정되어 있습니다."),
  ACCOUNT_005(HttpStatus.BAD_REQUEST, "ACCOUNT_005", "증권 계좌만 주식 계좌로 설정할 수 있습니다."),

  // TRANSFER
  TRANSFER_001(HttpStatus.NOT_FOUND, "TRANSFER_001", "해당 이체 건을 찾을 수 없습니다."),
  TRANSFER_002(HttpStatus.BAD_REQUEST, "TRANSFER_002", "잔액이 부족합니다."),
  TRANSFER_003(HttpStatus.CONFLICT, "TRANSFER_003", "이미 처리 완료된 이체입니다."),
  TRANSFER_004(HttpStatus.FORBIDDEN, "TRANSFER_004", "본인 이체 건이 아닙니다."),

  // ORDER
  ORDER_001(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 가능 금액이 부족합니다."),
  ORDER_002(HttpStatus.BAD_REQUEST, "ORDER_002", "보유 수량이 부족합니다."),

  // STOCK
  STOCK_001(HttpStatus.NOT_FOUND, "STOCK_001", "증권 계좌를 찾을 수 없습니다."),

  // FAVORITE
  FAVORITE_001(HttpStatus.NOT_FOUND, "FAVORITE_001", "관심종목 없음"),
  FAVORITE_002(HttpStatus.CONFLICT, "FAVORITE_002", "이미 등록된 관심종목입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
