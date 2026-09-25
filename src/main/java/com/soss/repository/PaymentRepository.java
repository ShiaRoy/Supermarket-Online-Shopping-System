package com.soss.repository;

import com.soss.model.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);

    /** Expects a transaction reference to identify at most one payment. */
    Optional<Payment> findByTransactionReference(String transactionReference);
}
