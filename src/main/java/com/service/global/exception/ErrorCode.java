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

  // VALID
  VALID_001(HttpStatus.BAD_REQUEST, "VALID_001", "입력값이 올바르지 않습니다."),

  // ACCOUNT
  ACCOUNT_001(HttpStatus.NOT_FOUND, "ACCOUNT_001", "해당 계좌를 찾을 수 없습니다."),
  ACCOUNT_002(HttpStatus.FORBIDDEN, "ACCOUNT_002", "본인 계좌가 아닙니다."),
  ACCOUNT_003(HttpStatus.BAD_REQUEST, "ACCOUNT_003", "계좌 상태가 유효하지 않습니다."),

  // TRANSFER
  TRANSFER_001(HttpStatus.NOT_FOUND, "TRANSFER_001", "해당 이체 건을 찾을 수 없습니다."),
  TRANSFER_002(HttpStatus.BAD_REQUEST, "TRANSFER_002", "잔액이 부족합니다."),
  TRANSFER_003(HttpStatus.CONFLICT, "TRANSFER_003", "이미 처리 완료된 이체입니다."),
  TRANSFER_004(HttpStatus.FORBIDDEN, "TRANSFER_004", "본인 이체 건이 아닙니다."),

  // ORDER
  ORDER_001(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 가능 금액이 부족합니다."),
  ORDER_002(HttpStatus.BAD_REQUEST, "ORDER_002", "보유 수량이 부족합니다."),

  // STOCK
  STOCK_001(HttpStatus.NOT_FOUND, "STOCK_001", "증권 계좌를 찾을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
