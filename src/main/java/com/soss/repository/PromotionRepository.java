package com.soss.repository;
import com.soss.model.Promotion;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    @EntityGraph(attributePaths = "product")
    List<Promotion> findAllByOrderByStartsOnDescIdDesc();
    @EntityGraph(attributePaths = "product")
    List<Promotion> findByProductIdAndEnabledTrue(Long productId);
}
