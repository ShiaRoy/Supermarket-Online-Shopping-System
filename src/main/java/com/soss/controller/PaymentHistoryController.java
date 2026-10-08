package com.soss.controller;

import com.soss.model.Order;
import com.soss.service.AccountService;
import com.soss.service.OrderService;
import com.soss.service.PaymentService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Customer-facing payment history. API endpoints remain in PaymentController. */
@Controller
public class PaymentHistoryController {

    private final AccountService accounts;
    private final OrderService orders;
    private final PaymentService payments;

    public PaymentHistoryController(AccountService accounts, OrderService orders,
            PaymentService payments) {
        this.accounts = accounts;
        this.orders = orders;
        this.payments = payments;
    }

    @GetMapping("/payment-history")
    public String history(Principal principal, Model model) {
        var customer = accounts.profile(principal.getName());
        List<Order> customerOrders = orders.getOrdersByCustomerId(customer.getId());
        List<Long> orderIds = customerOrders.stream().map(Order::getId).toList();
        Map<Long, Order> ordersById = customerOrders.stream()
                .collect(java.util.stream.Collectors.toMap(Order::getId, Function.identity()));

        model.addAttribute("payments", payments.getPaymentHistory(orderIds));
        model.addAttribute("ordersById", ordersById);
        return "payment-history";
    }
}
