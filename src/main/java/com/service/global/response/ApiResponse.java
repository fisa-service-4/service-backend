package com.service.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private final boolean success;
  private final T data;
  private final ErrorResponse error;
  private final Meta meta;

  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder().success(true).data(data).meta(Meta.create()).build();
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return ApiResponse.<T>builder()
        .success(false)
        .error(ErrorResponse.of(code, message))
        .meta(Meta.create())
        .build();
  }

  @Getter
  @Builder
  public static class ErrorResponse {

    private final String code;
    private final String message;

    public static ErrorResponse of(String code, String message) {
      return ErrorResponse.builder().code(code).message(message).build();
    }
  }

  @Getter
  @Builder
  public static class Meta {

    private final String traceId;

    public static Meta create() {
      return Meta.builder().traceId(UUID.randomUUID().toString()).build();
    }
  }
}
