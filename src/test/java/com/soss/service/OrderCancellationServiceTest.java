package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.OrderStatus;
import com.soss.model.Store;
import com.soss.model.delivery.Delivery;
import com.soss.repository.OrderRepository;
import com.soss.service.delivery.DeliveryService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock
    private OrderRepository orders;
    @Mock
    private CatalogueService catalogue;
    @Mock
    private DeliveryService deliveryService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private Store store;

    @InjectMocks
    private OrderCancellationService cancellationService;

    @Test
    void ownerCancelsEligibleOrderAndRestoresDeductedStockOnce() {
        Order order = paidOrder();
        when(orders.lockByIdWithItems(7L)).thenReturn(Optional.of(order));
        when(catalogue.store(null)).thenReturn(store);
        when(store.getId()).thenReturn(3L);
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = cancellationService.cancel(7L, 11L);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.isInventoryDeducted()).isFalse();
        verify(deliveryService).cancelForOrder(7L);
        verify(catalogue).restoreStock(21L, 3L, 2);
        verify(paymentService).recordRefundForOrder(7L);
        verify(orders).save(order);
    }

    @Test
    void anotherCustomerCannotCancelTheOrder() {
        Order order = paidOrder();
        when(orders.lockByIdWithItems(7L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> cancellationService.cancel(7L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own orders");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verifyNoInteractions(catalogue, deliveryService, paymentService);
        verify(orders, never()).save(any());
    }

    @Test
    void completedOrPreviouslyCancelledOrdersCannotBeCancelledOrRestockedAgain() {
        Order completed = paidOrder();
        completed.setStatus(OrderStatus.COMPLETED);
        when(orders.lockByIdWithItems(7L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> cancellationService.cancel(7L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Completed");

        Order cancelled = paidOrder();
        cancelled.setStatus(OrderStatus.CANCELLED);
        when(orders.lockByIdWithItems(8L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> cancellationService.cancel(8L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been cancelled");

        verifyNoInteractions(catalogue, deliveryService, paymentService);
        verify(orders, never()).save(any());
    }

    @Test
    void ordersWithoutDeductedStockNeverRestoreInventory() {
        Order order = paidOrder();
        order.setInventoryDeducted(false);
        when(orders.lockByIdWithItems(7L)).thenReturn(Optional.of(order));
        when(deliveryService.findByOrderId(7L)).thenReturn(Optional.empty());
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cancellationService.cancel(7L, 11L);

        verify(deliveryService).cancelForOrder(7L);
        verifyNoInteractions(catalogue);
        verify(paymentService).recordRefundForOrder(7L);
    }

    @Test
    void legacyCheckoutWithEligibleDeliveryRestoresStockWhenInventoryMarkerIsMissing() {
        Order order = paidOrder();
        order.setInventoryDeducted(false);
        Delivery delivery = new Delivery(7L, "Suva", null, null);
        when(orders.lockByIdWithItems(7L)).thenReturn(Optional.of(order));
        when(deliveryService.findByOrderId(7L)).thenReturn(Optional.of(delivery));
        when(catalogue.store(null)).thenReturn(store);
        when(store.getId()).thenReturn(3L);
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = cancellationService.cancel(7L, 11L);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(deliveryService).cancelForOrder(7L);
        verify(catalogue).restoreStock(21L, 3L, 2);
        verify(paymentService).recordRefundForOrder(7L);
    }

    private Order paidOrder() {
        Order order = new Order();
        order.setId(7L);
        order.setCustomerId(11L);
        order.setStatus(OrderStatus.PAID);
        order.setInventoryDeducted(true);
        OrderItem item = new OrderItem();
        item.setProductId(21L);
        item.setProductName("Rice");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("4.50"));
        order.addItem(item);
        return order;
    }
}
