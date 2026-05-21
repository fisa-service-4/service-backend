package com.service.global.exception;

import lombok.Getter;

@Getter

// 커스텀 예외 클래스
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
