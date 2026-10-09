package com.soss.repository.delivery;

import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.orderId = :orderId")
    Optional<Delivery> lockByOrderId(@Param("orderId") Long orderId);

    List<Delivery> findAllByOrderByUpdatedAtDesc();

    List<Delivery> findByStatusOrderByUpdatedAtDesc(DeliveryStatus status);

    List<Delivery> findByAssignedStaffIdOrderByUpdatedAtDesc(Long assignedStaffId);

    List<Delivery> findByAssignedStaffIdAndStatusOrderByUpdatedAtDesc(Long assignedStaffId, DeliveryStatus status);
}
