package com.service.global.exception;

import com.service.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    log.error("BusinessException: {}", e.getMessage());
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    BindingResult bindingResult = e.getBindingResult();
    String message = bindingResult.getFieldErrors().get(0).getDefaultMessage();
    return ResponseEntity.status(ErrorCode.VALID_001.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.VALID_001.getCode(), message));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(
      MissingRequestHeaderException e) {
    log.error("MissingRequestHeaderException: {}", e.getMessage());
    return ResponseEntity.status(ErrorCode.VALID_001.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.VALID_001.getCode(), e.getHeaderName() + " 헤더가 필요합니다."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("Exception: {}", e.getMessage());
    return ResponseEntity.status(500).body(ApiResponse.fail("SERVER_ERROR", "서버 오류가 발생했습니다."));
  }
}
