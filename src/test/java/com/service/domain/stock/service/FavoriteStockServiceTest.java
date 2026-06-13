package com.service.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.service.domain.stock.dto.request.FavoriteStockAddRequest;
import com.service.domain.stock.dto.response.FavoriteStockAddResponse;
import com.service.domain.stock.dto.response.FavoriteStockListResponse;
import com.service.domain.stock.entity.FavoriteStock;
import com.service.domain.stock.repository.FavoriteStockRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.client.TransactionServerClient.StockPriceItem;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteStockServiceTest {

  @InjectMocks private FavoriteStockService favoriteStockService;

  @Mock private FavoriteStockRepository favoriteStockRepository;

  @Mock private TransactionServerClient transactionServerClient;

  @Test
  @DisplayName("관심 종목 추가 시 저장된 엔티티를 응답으로 반환한다")
  void addFavorite_savesAndReturnsResponse() {
    Long userId = 1L;
    FavoriteStockAddRequest request = new FavoriteStockAddRequest();
    ReflectionTestUtils.setField(request, "stockCode", "005930");

    given(favoriteStockRepository.existsByUserIdAndStockCode(userId, "005930")).willReturn(false);

    FavoriteStock savedEntity =
        FavoriteStock.builder().userId(userId).stockCode("005930").build();
    ReflectionTestUtils.setField(savedEntity, "favoriteStockId", 10L);
    given(favoriteStockRepository.save(any(FavoriteStock.class))).willReturn(savedEntity);

    FavoriteStockAddResponse response = favoriteStockService.addFavorite(userId, request);

    assertThat(response.getFavoriteId()).isEqualTo(10L);
    assertThat(response.getStockCode()).isEqualTo("005930");
    then(favoriteStockRepository).should().save(any(FavoriteStock.class));
  }

  @Test
  @DisplayName("이미 등록된 관심 종목 추가 시 FAVORITE_002 예외가 발생한다")
  void addFavorite_throwsExceptionWhenDuplicate() {
    Long userId = 1L;
    FavoriteStockAddRequest request = new FavoriteStockAddRequest();
    ReflectionTestUtils.setField(request, "stockCode", "005930");

    given(favoriteStockRepository.existsByUserIdAndStockCode(userId, "005930")).willReturn(true);

    assertThatThrownBy(() -> favoriteStockService.addFavorite(userId, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.FAVORITE_002);
  }

  @Test
  @DisplayName("관심 종목 삭제 시 정상적으로 삭제된다")
  void deleteFavorite_deletesEntitySuccessfully() {
    Long userId = 1L;
    Long favoriteId = 10L;
    FavoriteStock entity =
        FavoriteStock.builder().userId(userId).stockCode("005930").build();

    given(favoriteStockRepository.findByFavoriteStockIdAndUserId(favoriteId, userId))
        .willReturn(Optional.of(entity));

    favoriteStockService.deleteFavorite(userId, favoriteId);

    then(favoriteStockRepository).should().delete(entity);
  }

  @Test
  @DisplayName("존재하지 않는 관심 종목 삭제 시 VALID_001 예외가 발생한다")
  void deleteFavorite_throwsExceptionWhenNotFound() {
    Long userId = 1L;
    Long favoriteId = 999L;

    given(favoriteStockRepository.findByFavoriteStockIdAndUserId(favoriteId, userId))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> favoriteStockService.deleteFavorite(userId, favoriteId))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.VALID_001);
  }

  @Test
  @DisplayName("관심 종목 목록 조회 시 각 종목의 현재가 정보를 포함하여 반환한다")
  void getFavorites_returnsListWithCurrentPrice() {
    Long userId = 1L;
    FavoriteStock favorite =
        FavoriteStock.builder().userId(userId).stockCode("005930").build();
    ReflectionTestUtils.setField(favorite, "favoriteStockId", 10L);

    StockPriceItem priceItem = new StockPriceItem();
    ReflectionTestUtils.setField(priceItem, "stockCode", "005930");
    ReflectionTestUtils.setField(priceItem, "stockName", "삼성전자");
    ReflectionTestUtils.setField(priceItem, "currentPrice", new BigDecimal("75000"));
    ReflectionTestUtils.setField(priceItem, "changeRate", new BigDecimal("1.5"));

    given(favoriteStockRepository.findAllByUserId(userId)).willReturn(List.of(favorite));
    given(transactionServerClient.getStockPrice("005930")).willReturn(priceItem);

    FavoriteStockListResponse response = favoriteStockService.getFavorites(userId);

    assertThat(response.getFavorites()).hasSize(1);
    assertThat(response.getFavorites().get(0).getStockCode()).isEqualTo("005930");
    assertThat(response.getFavorites().get(0).getCurrentPrice())
        .isEqualByComparingTo(new BigDecimal("75000"));
  }

  @Test
  @DisplayName("관심 종목이 없을 때 빈 목록을 반환한다")
  void getFavorites_returnsEmptyListWhenNoFavorites() {
    Long userId = 1L;
    given(favoriteStockRepository.findAllByUserId(userId)).willReturn(List.of());

    FavoriteStockListResponse response = favoriteStockService.getFavorites(userId);

    assertThat(response.getFavorites()).isEmpty();
  }
}
