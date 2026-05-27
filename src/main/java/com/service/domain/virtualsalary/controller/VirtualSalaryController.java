package com.service.domain.virtualsalary.controller;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;
import com.service.domain.virtualsalary.dto.response.ContractDetailResponse;
import com.service.domain.virtualsalary.dto.response.ContractListResponse;
import com.service.domain.virtualsalary.service.ContractService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        description = "VALID_001: 입력값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @PostMapping("/contracts")
  public ResponseEntity<ApiResponse<ContractCreateResponse>> createContract(
      Authentication authentication, @Valid @RequestBody ContractCreateRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(contractService.createContract(userId, request)));
  }

  @Operation(summary = "계약 목록 조회", description = "월별 계약 목록을 조회합니다. date 미입력 시 현재 월 기준으로 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "계약 목록 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/contracts")
  public ResponseEntity<ApiResponse<List<ContractListResponse>>> getContracts(
      Authentication authentication, @RequestParam(required = false) LocalDate date) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(contractService.getContracts(userId, date)));
  }

  @Operation(summary = "계약 상세 조회", description = "계약 ID로 계약 상세 정보를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "계약 상세 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "CONTRACT_001: 존재하지 않는 계약")
  })
  @GetMapping("/contracts/{contractId}")
  public ResponseEntity<ApiResponse<ContractDetailResponse>> getContractDetail(
      Authentication authentication, @PathVariable Long contractId) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(contractService.getContractDetail(userId, contractId)));
  }
}
