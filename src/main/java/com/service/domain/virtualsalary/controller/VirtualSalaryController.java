package com.service.domain.virtualsalary.controller;

import com.service.domain.virtualsalary.dto.request.ContractCreateRequest;
import com.service.domain.virtualsalary.dto.request.VirtualSalarySettingRequest;
import com.service.domain.virtualsalary.dto.response.ContractCreateResponse;
import com.service.domain.virtualsalary.dto.response.ContractDetailResponse;
import com.service.domain.virtualsalary.dto.response.ContractListResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalaryDashboardResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalaryRecommendationResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySaveResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySettingResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySummaryResponse;
import com.service.domain.virtualsalary.facade.VirtualSalaryFacade;
import com.service.domain.virtualsalary.service.ContractService;
import com.service.domain.virtualsalary.service.VirtualSalaryDashboardService;
import com.service.domain.virtualsalary.service.VirtualSalaryRecommendationService;
import com.service.domain.virtualsalary.service.VirtualSalarySettingService;
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

@Tag(name = "VirtualSalary", description = "가상월급 및 계약 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VirtualSalaryController {

  private final ContractService contractService;
  private final VirtualSalarySettingService virtualSalarySettingService;
  private final VirtualSalaryDashboardService virtualSalaryDashboardService;
  private final VirtualSalaryFacade virtualSalaryFacade;
  private final VirtualSalaryRecommendationService virtualSalaryRecommendationService;

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

  // ─── Virtual Salary Summary ───────────────────────────────────────────────

  @Operation(
      summary = "가상월급 홈 통합 조회",
      description = "가상월급 메인 화면에 필요한 대시보드, 이번달 계약 목록, 예정 수입, 캘린더 데이터를 통합 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "통합 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "VIRTUAL_SALARY_001: 가상월급 설정 없음 | VIRTUAL_SALARY_003: SALARY 계좌 미연결")
  })
  @GetMapping("/virtual-salary/summary")
  public ResponseEntity<ApiResponse<VirtualSalarySummaryResponse>> getSummary(
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(virtualSalaryFacade.getSummary(userId)));
  }

  // ─── Virtual Salary Recommendation ───────────────────────────────────────

  @Operation(
      summary = "AI 분배 금액 추천 조회",
      description = "사용자의 계약 수입, 자산 현황, 가상월급 설정을 기반으로 AI 서버에서 비상금/투자 금액을 추천합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "AI 추천 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "VIRTUAL_SALARY_001: 가상월급 설정 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "AI_001: AI 응답 생성 실패 | AI_003: AI 실행 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "504",
        description = "AI_002: AI 서버 응답 시간 초과")
  })
  @GetMapping("/virtual-salary/recommendation")
  public ResponseEntity<ApiResponse<VirtualSalaryRecommendationResponse>> getRecommendation(
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(virtualSalaryRecommendationService.getRecommendation(userId)));
  }

  // ─── Virtual Salary Dashboard ─────────────────────────────────────────────

  @Operation(
      summary = "가상월급 대시보드 조회",
      description = "SALARY 계좌 잔액 기반으로 이번달 가상월급 사용 현황 및 D-DAY를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "대시보드 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "VIRTUAL_SALARY_001: 가상월급 설정 없음 | VIRTUAL_SALARY_003: SALARY 계좌 미연결")
  })
  @GetMapping("/virtual-salary/dashboard")
  public ResponseEntity<ApiResponse<VirtualSalaryDashboardResponse>> getDashboard(
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(
        ApiResponse.success(virtualSalaryDashboardService.getDashboard(userId)));
  }

  // ─── Virtual Salary Setting ────────────────────────────────────────────────

  @Operation(
      summary = "가상월급 설정 조회",
      description = "현재 사용자의 가상월급 설정을 조회합니다. 설정이 없으면 모든 필드가 null인 빈 응답을 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "가상월급 설정 조회 성공 (설정 미존재 시 빈 응답 반환)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping("/virtual-salary")
  public ResponseEntity<ApiResponse<VirtualSalarySettingResponse>> getVirtualSalarySetting(
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(virtualSalarySettingService.getSetting(userId)));
  }

  @Operation(
      summary = "가상월급 설정 저장",
      description = "가상월급 설정을 저장합니다. 기존 설정이 없으면 생성하고, 있으면 덮어씁니다(upsert).")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "가상월급 설정 저장 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "VALID_001: 입력값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @PostMapping("/virtual-salary")
  public ResponseEntity<ApiResponse<VirtualSalarySaveResponse>> saveVirtualSalarySetting(
      Authentication authentication, @Valid @RequestBody VirtualSalarySettingRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(virtualSalarySettingService.saveSetting(userId, request)));
  }
}
