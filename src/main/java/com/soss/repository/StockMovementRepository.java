package com.soss.repository;

import com.soss.model.StockMovement;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByProductIdAndStoreIdOrderByOccurredAtDescIdDesc(Long productId, Long storeId, Pageable page);
}
