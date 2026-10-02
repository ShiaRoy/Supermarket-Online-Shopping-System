package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderStatus;
import com.soss.model.Payment;
import com.soss.model.PaymentStatus;
import com.soss.repository.OrderRepository;
import com.soss.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Payment simulatePayment(Long orderId, boolean shouldSucceed) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        if (order.getTotalAmount() == null) {
            throw new IllegalArgumentException("Order total must not be null for order ID: " + orderId);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Order must be PENDING_PAYMENT to simulate payment: " + orderId);
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(order.getTotalAmount());
        payment.setTransactionReference(UUID.randomUUID().toString());

        if (shouldSucceed) {
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Simulated payment failure");
            order.setStatus(OrderStatus.PAYMENT_FAILED);
        }

        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);
        return savedPayment;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findPaymentByTransactionReference(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new IllegalArgumentException("Transaction reference must not be null or blank");
        }
        return paymentRepository.findByTransactionReference(transactionReference);
    }
}
