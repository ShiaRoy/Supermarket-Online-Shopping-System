package com.soss.service;

import com.soss.model.Order;
import com.soss.model.Payment;
import com.soss.model.PaymentStatus;
import com.soss.model.PaymentTransactionType;
import com.soss.model.Role;
import com.soss.model.User;
import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.repository.OrderRepository;
import com.soss.repository.PaymentRepository;
import com.soss.repository.UserRepository;
import com.soss.repository.delivery.DeliveryRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final DeliveryRepository deliveries;
    private final CatalogueService catalogue;

    public AdminDashboardService(OrderRepository orders, PaymentRepository payments, UserRepository users,
            DeliveryRepository deliveries, CatalogueService catalogue) {
        this.orders = orders;
        this.payments = payments;
        this.users = users;
        this.deliveries = deliveries;
        this.catalogue = catalogue;
    }

    public record RecentOrder(Long id, Long customerId, java.time.LocalDateTime orderDate,
                              BigDecimal totalAmount, String status) {}
    public record Dashboard(long totalOrders, BigDecimal netSuccessfulSales, long lowStockAlerts,
                            long registeredCustomers, long pendingDeliveries, List<RecentOrder> recentOrders) {}
    public record PaymentRow(String type, String method, BigDecimal amount, String status,
                             java.time.LocalDateTime date, String reference) {}
    public record OrderRow(Long id, Long customerId, String customerName, String customerEmail,
                           java.time.LocalDateTime date, BigDecimal total, String status, List<PaymentRow> payments) {}

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        List<Payment> history = payments.findAll();
        BigDecimal sales = history.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS
                        && payment.getTransactionType() != PaymentTransactionType.REFUND)
                .map(Payment::getAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = history.stream()
                .filter(payment -> payment.getTransactionType() == PaymentTransactionType.REFUND
                        && payment.getStatus() == PaymentStatus.REFUNDED)
                .map(Payment::getAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStock = catalogue.stores().stream()
                .mapToLong(store -> catalogue.search(store.getId(), "", "", null, null, "alerts", true).size())
                .sum();
        long pending = deliveries.findAllByOrderByUpdatedAtDesc().stream()
                .filter(delivery -> delivery.getCancelledAt() == null)
                .filter(delivery -> delivery.getStatus() != DeliveryStatus.DELIVERED)
                .count();
        List<RecentOrder> recent = orders.findTop5ByOrderByOrderDateDesc().stream()
                .map(this::recent).toList();
        return new Dashboard(orders.count(), sales.subtract(refunds).max(BigDecimal.ZERO), lowStock,
                users.countByRole(Role.CUSTOMER), pending, recent);
    }

    @Transactional(readOnly = true)
    public List<OrderRow> orders() {
        Map<Long, User> customers = users.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, ignored) -> first));
        Map<Long, List<PaymentRow>> byOrder = payments.findAll().stream()
                .collect(Collectors.groupingBy(Payment::getOrderId,
                        Collectors.mapping(this::paymentRow, Collectors.toList())));
        return orders.findAll().stream()
                .sorted(Comparator.comparing(Order::getOrderDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(order -> {
                    User customer = customers.get(order.getCustomerId());
                    return new OrderRow(order.getId(), order.getCustomerId(),
                            customer == null ? "Account unavailable" : customer.getFullName(),
                            customer == null ? "" : customer.getEmail(), order.getOrderDate(),
                            order.getTotalAmount(), order.getStatus() == null ? "UNKNOWN" : order.getStatus().name(),
                            byOrder.getOrDefault(order.getId(), List.of()));
                }).toList();
    }

    private RecentOrder recent(Order order) {
        return new RecentOrder(order.getId(), order.getCustomerId(), order.getOrderDate(),
                order.getTotalAmount(), order.getStatus() == null ? "UNKNOWN" : order.getStatus().name());
    }

    private PaymentRow paymentRow(Payment payment) {
        String type = payment.getTransactionType() == null ? "PAYMENT" : payment.getTransactionType().name();
        return new PaymentRow(type, payment.getPaymentMethod() == null ? "—" : payment.getPaymentMethod(),
                payment.getAmount(), payment.getStatus() == null ? "UNKNOWN" : payment.getStatus().name(),
                payment.getPaymentDate(), payment.getTransactionReference());
    }
}