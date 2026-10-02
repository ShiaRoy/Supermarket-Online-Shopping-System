package com.soss.checkout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderRepository extends JpaRepository<ShopOrder,Long> {
 Optional<ShopOrder> findByOwnerAndCheckoutToken(String owner,String token);
 Optional<ShopOrder> findByIdAndOwner(Long id,String owner);
 List<ShopOrder> findByOwnerOrderByCreatedAtDesc(String owner);
}

