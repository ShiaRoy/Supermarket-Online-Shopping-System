package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.User;
import com.soss.repository.OrderRepository;
import com.soss.repository.UserRepository;
import com.soss.service.delivery.DeliveryService;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCheckoutOrders implements CheckoutOrders {

    private final UserRepository users;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CatalogueService catalogue;
    private final DeliveryService deliveryService;

    public OrderCheckoutOrders(
            UserRepository users,
            OrderRepository orders,
            OrderService orderService,
            PaymentService paymentService,
            CatalogueService catalogue,
            DeliveryService deliveryService) {

        this.users = users;
        this.orders = orders;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.catalogue = catalogue;
        this.deliveryService = deliveryService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> findExisting(String owner, String checkoutToken) {

        validateIdentity(owner, checkoutToken);

        User customer = users.findByEmail(owner)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer account not found."));

        return orders
                .findByCustomerIdAndCheckoutToken(
                        customer.getId(),
                        checkoutToken)
                .map(order -> new Receipt(order.getId()));
    }

    @Override
    @Transactional
    public Receipt place(Request request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Checkout request must not be null.");
        }

        validateIdentity(
                request.owner(),
                request.checkoutToken());

        /*
         * Lock the customer row so two checkout requests cannot
         * create the same order at the same time.
         */
        User customer = users
                .lockByEmail(request.owner())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer account not found."));

        Optional<Order> existing =
                orders.findByCustomerIdAndCheckoutToken(
                        customer.getId(),
                        request.checkoutToken());

        if (existing.isPresent()) {
            return new Receipt(existing.get().getId());
        }

        if (request.total() == null
                || request.total().signum() < 0) {

            throw new IllegalArgumentException(
                    "Checkout total must not be null or negative.");
        }

        if (request.items() == null
                || request.items().isEmpty()) {

            throw new IllegalArgumentException(
                    "Checkout must contain at least one item.");
        }

        Long storeId = catalogue.store(null).getId();

        Order order = new Order();

        order.setCustomerId(customer.getId());
        order.setCheckoutToken(request.checkoutToken());

        for (Item requestedItem : request.items()) {

            if (requestedItem == null
                    || requestedItem.productId() == null) {

                throw new IllegalArgumentException(
                        "Checkout item must identify a product.");
            }

            OrderItem item = new OrderItem();

            item.setProductId(requestedItem.productId());
            item.setProductName(requestedItem.name());
            item.setQuantity(requestedItem.quantity());
            item.setUnitPrice(requestedItem.unitPrice());

            order.addItem(item);
        }

        /*
         * 1. Create shared order.
         */
        Order savedOrder =
                orderService.createOrder(order);

        /*
         * OrderService calculates the merchandise subtotal.
         * Checkout total also includes delivery and discounts.
         */
        savedOrder.setTotalAmount(
                request.total()
                        .setScale(
                                2,
                                RoundingMode.UNNECESSARY));

        orders.save(savedOrder);

        /*
         * 2. Process simulated payment.
         */
        paymentService.simulatePayment(
                savedOrder.getId(),
                true);

        /*
         * 3. Deduct real inventory stock.
         */
        for (Item item : request.items()) {

            catalogue.deductStock(
                    item.productId(),
                    storeId,
                    item.quantity());
        }

        /*
         * 4. Create Delivery record automatically.
         */
        DeliveryWindow window =
                parseDeliverySlot(request.deliverySlot());

        deliveryService.createForOrder(
                savedOrder.getId(),
                request.address(),
                window.from(),
                window.to());

        return new Receipt(savedOrder.getId());
    }

    private DeliveryWindow parseDeliverySlot(
            String deliverySlot) {

        if (deliverySlot == null
                || deliverySlot.isBlank()) {

            return new DeliveryWindow(null, null);
        }

        String[] parts =
                deliverySlot.split("\\s\\|\\s", 2);

        if (parts.length != 2) {
            return new DeliveryWindow(null, null);
        }

        LocalDate date;

        try {
            date = LocalDate.parse(parts[0].trim());
        } catch (Exception e) {
            return new DeliveryWindow(null, null);
        }

        String time =
                parts[1].trim();

        if (time.equals("10am – 12pm")) {

            return new DeliveryWindow(
                    LocalDateTime.of(
                            date,
                            LocalTime.of(10, 0)),
                    LocalDateTime.of(
                            date,
                            LocalTime.of(12, 0)));
        }

        if (time.equals("2pm – 4pm")) {

            return new DeliveryWindow(
                    LocalDateTime.of(
                            date,
                            LocalTime.of(14, 0)),
                    LocalDateTime.of(
                            date,
                            LocalTime.of(16, 0)));
        }

        return new DeliveryWindow(null, null);
    }

    private void validateIdentity(
            String owner,
            String checkoutToken) {

        if (owner == null
                || owner.isBlank()) {

            throw new IllegalArgumentException(
                    "Customer email must not be blank.");
        }

        if (checkoutToken == null
                || checkoutToken.isBlank()
                || checkoutToken.length() > 36) {

            throw new IllegalArgumentException(
                    "A valid checkout token is required.");
        }
    }

    private record DeliveryWindow(
            LocalDateTime from,
            LocalDateTime to) {
    }
}
