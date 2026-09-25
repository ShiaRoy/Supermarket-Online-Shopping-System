package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.OrderStatus;
import com.soss.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderCalculatesTotalAndLinksItemsBeforeSaving() {
        Order order = new Order();
        order.setCustomerId(10L);
        OrderItem firstItem = item(2, "12.35");
        OrderItem secondItem = item(3, "0.10");
        // Leave back-references unset to verify that the service establishes them.
        order.getItems().add(firstItem);
        order.getItems().add(secondItem);
        order.setTotalAmount(new BigDecimal("999.99"));
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            assertThat(saved.getTotalAmount()).isEqualByComparingTo("25.00");
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(firstItem.getOrder()).isSameAs(saved);
            assertThat(secondItem.getOrder()).isSameAs(saved);
            return saved;
        });

        Order result = orderService.createOrder(order);

        assertThat(result).isSameAs(order);
        verify(orderRepository).save(order);
    }

    @Test
    void createOrderRejectsNullCustomerIdWithoutSaving() {
        Order order = validOrder();
        order.setCustomerId(null);

        assertThatThrownBy(() -> orderService.createOrder(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrderRejectsEmptyItemsWithoutSaving() {
        Order order = new Order();
        order.setCustomerId(10L);

        assertThatThrownBy(() -> orderService.createOrder(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
        verifyNoInteractions(orderRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void createOrderRejectsInvalidQuantityWithoutSaving(Integer quantity) {
        Order order = validOrder();
        order.getItems().getFirst().setQuantity(quantity);

        assertThatThrownBy(() -> orderService.createOrder(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrderRejectsNegativeUnitPriceWithoutSaving() {
        Order order = validOrder();
        order.getItems().getFirst().setUnitPrice(new BigDecimal("-0.01"));

        assertThatThrownBy(() -> orderService.createOrder(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit price");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrderByIdReturnsExistingOrder() {
        Order order = validOrder();
        order.setId(20L);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        assertThat(orderService.getOrderById(20L)).isSameAs(order);
        verify(orderRepository).findById(20L);
    }

    @Test
    void getOrderByIdRejectsMissingOrder() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found")
                .hasMessageContaining("404");
        verify(orderRepository).findById(404L);
    }

    @Test
    void updateOrderStatusSavesUpdatedOrder() {
        Order order = validOrder();
        order.setId(20L);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.updateOrderStatus(20L, OrderStatus.PROCESSING);

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrder.capture());
        assertThat(savedOrder.getValue()).isSameAs(order);
        assertThat(savedOrder.getValue().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(result).isSameAs(order);
    }

    private Order validOrder() {
        Order order = new Order();
        order.setCustomerId(10L);
        order.addItem(item(2, "12.35"));
        return order;
    }

    private OrderItem item(Integer quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }
}
