package com.service.global.exception;

import com.service.domain.admin.service.AdminLogSaveService;
import com.service.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final AdminLogSaveService adminLogSaveService;

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(
      BusinessException e, HttpServletRequest request) {
    log.error("BusinessException: {}", e.getMessage());
    ErrorCode errorCode = e.getErrorCode();
    String errorLevel = errorCode.getHttpStatus().is5xxServerError() ? "ERROR" : "WARN";
    adminLogSaveService.saveSystemErrorLog(
        getOrCreateTraceId(request),
        errorLevel,
        errorCode.getCode(),
        errorCode.getMessage(),
        request.getRequestURI());
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
  public ResponseEntity<ApiResponse<Void>> handleException(
      Exception e, HttpServletRequest request) {
    log.error("Exception: {}", e.getMessage());
    adminLogSaveService.saveSystemErrorLog(
        getOrCreateTraceId(request),
        "ERROR",
        "SERVER_ERROR",
        e.getMessage() != null ? e.getMessage() : "알 수 없는 오류",
        request.getRequestURI());
    return ResponseEntity.status(500).body(ApiResponse.fail("SERVER_ERROR", "서버 오류가 발생했습니다."));
  }

  private String getOrCreateTraceId(HttpServletRequest request) {
    String traceId = (String) request.getAttribute("traceId");
    return traceId != null ? traceId : UUID.randomUUID().toString();
  }
}
