package com.service.domain.stock.repository;

import com.service.domain.stock.entity.FavoriteStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteStockRepository extends JpaRepository<FavoriteStock, Long> {

  List<FavoriteStock> findAllByUserId(Long userId);

  Optional<FavoriteStock> findByFavoriteStockIdAndUserId(Long favoriteStockId, Long userId);
}
