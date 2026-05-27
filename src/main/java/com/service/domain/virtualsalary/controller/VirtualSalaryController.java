package com.service.domain.virtualsalary.controller;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;
import com.service.domain.virtualsalary.service.ContractService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Contract", description = "계약 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VirtualSalaryController {

  private final ContractService contractService;

  @Operation(summary = "계약 생성", description = "프리랜서 계약 정보를 등록하고 세금 정산을 계산합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "계약 생성 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "입력값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  @PostMapping("/contracts")
  public ResponseEntity<ApiResponse<ContractCreateResponse>> createContract(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody ContractCreateRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(contractService.createContract(userId, request)));
  }
}
