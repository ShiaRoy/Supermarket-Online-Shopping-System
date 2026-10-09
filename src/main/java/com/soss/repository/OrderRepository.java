package com.soss.repository;

import com.soss.model.Order;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);
    List<Order> findAllByOrderByOrderDateDesc();
    List<Order> findTop5ByOrderByOrderDateDesc();

    Optional<Order> findByCustomerIdAndCheckoutToken(Long customerId, String checkoutToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o left join fetch o.items where o.id = :orderId")
    Optional<Order> lockByIdWithItems(@Param("orderId") Long orderId);
}
