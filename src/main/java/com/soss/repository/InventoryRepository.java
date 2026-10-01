package com.soss.repository;

import com.soss.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @EntityGraph(attributePaths = {"product", "store"})
    List<Inventory> findByStoreIdOrderByProductNameAsc(Long storeId);
    @EntityGraph(attributePaths = {"product", "store"})
    Optional<Inventory> findByProductIdAndStoreId(Long productId, Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i join fetch i.product where i.product.id = :productId and i.store.id = :storeId")
    Optional<Inventory> lockStock(@Param("productId") Long productId, @Param("storeId") Long storeId);
}
