package com.soss.controller;

import com.soss.model.Payment;
import com.soss.model.Order;
import com.soss.model.User;
import com.soss.service.AccountService;
import com.soss.service.OrderService;
import com.soss.service.PaymentService;
import java.security.Principal;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
@ResponseBody
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final AccountService accountService;

    public PaymentController(
            PaymentService paymentService,
            OrderService orderService,
            AccountService accountService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.accountService = accountService;
    }

    @PostMapping("/orders/{orderId}/simulate-success")
    public Payment simulateSuccessfulPayment(@PathVariable Long orderId, Principal principal) {
        requireOwnedOrder(orderId, principal);
        return paymentService.simulatePayment(orderId, true);
    }

    @PostMapping("/orders/{orderId}/simulate-failure")
    public Payment simulateFailedPayment(@PathVariable Long orderId, Principal principal) {
        requireOwnedOrder(orderId, principal);
        return paymentService.simulatePayment(orderId, false);
    }

    @GetMapping("/orders/{orderId}")
    public List<Payment> getOrderPayments(@PathVariable Long orderId, Principal principal) {
        requireOwnedOrder(orderId, principal);
        return paymentService.getPaymentsByOrderId(orderId);
    }

    @GetMapping("/reference/{transactionReference}")
    public ResponseEntity<Payment> getPaymentByTransactionReference(
            @PathVariable String transactionReference,
            Principal principal) {
        Payment payment = paymentService.findPaymentByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        requireOwnedOrder(payment.getOrderId(), principal);
        return ResponseEntity.ok(payment);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidPayment(IllegalArgumentException exception) {
        return Map.of("error", exception.getMessage());
    }

    private void requireOwnedOrder(Long orderId, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required.");
        }
        User customer = accountService.profile(principal.getName());
        Order order = orderService.getOrderById(orderId);
        if (!Objects.equals(order.getCustomerId(), customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your payment.");
        }
    }
}
