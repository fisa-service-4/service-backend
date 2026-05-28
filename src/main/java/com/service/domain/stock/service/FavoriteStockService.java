package com.service.domain.stock.service;

import com.service.domain.stock.dto.request.FavoriteStockAddRequest;
import com.service.domain.stock.dto.response.FavoriteStockAddResponse;
import com.service.domain.stock.dto.response.FavoriteStockListResponse;
import com.service.domain.stock.dto.response.FavoriteStockListResponse.FavoriteStockItem;
import com.service.domain.stock.entity.FavoriteStock;
import com.service.domain.stock.repository.FavoriteStockRepository;
import com.service.global.client.TransactionServerClient;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteStockService {

  private final FavoriteStockRepository favoriteStockRepository;
  private final TransactionServerClient transactionServerClient;

  @Transactional
  public FavoriteStockAddResponse addFavorite(Long userId, FavoriteStockAddRequest request) {
    FavoriteStock entity =
        FavoriteStock.builder().userId(userId).stockCode(request.getStockCode()).build();
    return FavoriteStockAddResponse.from(favoriteStockRepository.save(entity));
  }

  @Transactional
  public void deleteFavorite(Long userId, Long favoriteId) {
    FavoriteStock entity =
        favoriteStockRepository
            .findByFavoriteStockIdAndUserId(favoriteId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VALID_001));
    favoriteStockRepository.delete(entity);
  }

  public FavoriteStockListResponse getFavorites(Long userId) {
    List<FavoriteStock> favorites = favoriteStockRepository.findAllByUserId(userId);
    List<FavoriteStockItem> items =
        favorites.stream()
            .map(
                f ->
                    FavoriteStockItem.of(
                        f.getFavoriteStockId(),
                        transactionServerClient.getStockPrice(f.getStockCode())))
            .toList();
    return FavoriteStockListResponse.builder().favorites(items).build();
  }
}
