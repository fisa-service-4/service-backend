package com.service.domain.stock.controller;

import com.service.domain.stock.dto.request.FavoriteStockAddRequest;
import com.service.domain.stock.dto.response.FavoriteStockAddResponse;
import com.service.domain.stock.dto.response.FavoriteStockListResponse;
import com.service.domain.stock.service.FavoriteStockService;
import com.service.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FavoriteStock", description = "관심종목 API")
@RestController
@RequestMapping("/api/v1/favorite-stocks")
@RequiredArgsConstructor
public class FavoriteStockController {

  private final FavoriteStockService favoriteStockService;

  @Operation(summary = "관심종목 등록")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "등록 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<FavoriteStockAddResponse>> addFavorite(
      Authentication authentication, @Valid @RequestBody FavoriteStockAddRequest request) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(favoriteStockService.addFavorite(userId, request)));
  }

  @Operation(summary = "관심종목 삭제")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "삭제 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @DeleteMapping("/{favoriteId}")
  public ResponseEntity<ApiResponse<Void>> deleteFavorite(
      Authentication authentication, @PathVariable Long favoriteId) {
    Long userId = (Long) authentication.getPrincipal();
    favoriteStockService.deleteFavorite(userId, favoriteId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "관심종목 목록 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "AUTH_004: 만료된 토큰 | AUTH_005: 유효하지 않은 토큰")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<FavoriteStockListResponse>> getFavorites(
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return ResponseEntity.ok(ApiResponse.success(favoriteStockService.getFavorites(userId)));
  }
}
