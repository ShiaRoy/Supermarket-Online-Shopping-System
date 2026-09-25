package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderStatus;
import com.soss.model.Payment;
import com.soss.model.PaymentStatus;
import com.soss.repository.OrderRepository;
import com.soss.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void successfulPaymentSavesPaymentAndMarksOrderPaid() {
        Order order = pendingOrder();
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.simulatePayment(20L, true);

        ArgumentCaptor<Payment> savedPayment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(savedPayment.capture());
        Payment payment = savedPayment.getValue();
        assertThat(result).isSameAs(payment);
        assertThat(payment.getOrderId()).isEqualTo(20L);
        assertThat(payment.getAmount()).isEqualByComparingTo(order.getTotalAmount());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionReference()).isNotBlank();
        assertThat(UUID.fromString(payment.getTransactionReference()).toString())
                .isEqualTo(payment.getTransactionReference());
        assertThat(payment.getFailureReason()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void failedPaymentSavesFailureAndUpdatesOrder() {
        Order order = pendingOrder();
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.simulatePayment(20L, false);

        ArgumentCaptor<Payment> savedPayment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(savedPayment.capture());
        assertThat(result).isSameAs(savedPayment.getValue());
        assertThat(result.getOrderId()).isEqualTo(20L);
        assertThat(result.getAmount()).isEqualByComparingTo(order.getTotalAmount());
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Simulated payment failure");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(orderRepository).save(order);
    }

    @Test
    void simulatePaymentRejectsMissingOrderWithoutSaving() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.simulatePayment(404L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found")
                .hasMessageContaining("404");
        verifyNoInteractions(paymentRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE,
            names = "PENDING_PAYMENT")
    void simulatePaymentRejectsNonPendingOrderWithoutSaving(OrderStatus status) {
        Order order = pendingOrder();
        order.setStatus(status);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.simulatePayment(20L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING_PAYMENT");
        assertThat(order.getStatus()).isEqualTo(status);
        verifyNoInteractions(paymentRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void simulatePaymentRejectsNullOrderTotalWithoutSaving() {
        Order order = pendingOrder();
        order.setTotalAmount(null);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.simulatePayment(20L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order total");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verifyNoInteractions(paymentRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getPaymentsByOrderIdReturnsRepositoryResults() {
        Payment first = new Payment();
        first.setOrderId(20L);
        Payment second = new Payment();
        second.setOrderId(20L);
        List<Payment> payments = List.of(first, second);
        when(paymentRepository.findByOrderId(20L)).thenReturn(payments);

        assertThat(paymentService.getPaymentsByOrderId(20L)).containsExactly(first, second);
        verify(paymentRepository).findByOrderId(20L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void findPaymentByTransactionReferenceReturnsMatchingPayment() {
        String reference = "81bdd1ad-7019-4b02-b24d-ced4869601bc";
        Payment payment = new Payment();
        payment.setTransactionReference(reference);
        when(paymentRepository.findByTransactionReference(reference)).thenReturn(Optional.of(payment));

        assertThat(paymentService.findPaymentByTransactionReference(reference)).containsSame(payment);
        verify(paymentRepository).findByTransactionReference(reference);
        verifyNoInteractions(orderRepository);
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId(20L);
        order.setTotalAmount(new BigDecimal("25.75"));
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        return order;
    }
}
