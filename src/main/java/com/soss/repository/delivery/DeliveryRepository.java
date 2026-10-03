package com.soss.repository.delivery;

import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);

    List<Delivery> findAllByOrderByUpdatedAtDesc();

    List<Delivery> findByStatusOrderByUpdatedAtDesc(DeliveryStatus status);
}
