package com.soss.repository;

import com.soss.model.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    Optional<Order> findByCustomerIdAndCheckoutToken(Long customerId, String checkoutToken);
}
