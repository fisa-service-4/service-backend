package com.service.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "오류 해결 처리 요청")
public class ErrorLogResolveRequest {

  @Schema(description = "해결 여부", example = "true")
  @NotNull
  private Boolean resolvedYn;

  @Schema(description = "해결 메모", example = "DB 연결 설정 수정으로 해결")
  private String resolvedMemo;
}
