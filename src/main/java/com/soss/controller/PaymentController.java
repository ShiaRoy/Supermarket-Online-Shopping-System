package com.soss.controller;

import com.soss.model.Payment;
import com.soss.service.PaymentService;
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

@Controller
@ResponseBody
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/simulate-success")
    public Payment simulateSuccessfulPayment(@PathVariable Long orderId) {
        return paymentService.simulatePayment(orderId, true);
    }

    @PostMapping("/orders/{orderId}/simulate-failure")
    public Payment simulateFailedPayment(@PathVariable Long orderId) {
        return paymentService.simulatePayment(orderId, false);
    }

    @GetMapping("/orders/{orderId}")
    public List<Payment> getOrderPayments(@PathVariable Long orderId) {
        return paymentService.getPaymentsByOrderId(orderId);
    }

    @GetMapping("/reference/{transactionReference}")
    public ResponseEntity<Payment> getPaymentByTransactionReference(
            @PathVariable String transactionReference) {
        return ResponseEntity.of(paymentService.findPaymentByTransactionReference(transactionReference));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidPayment(IllegalArgumentException exception) {
        return Map.of("error", exception.getMessage());
    }
}
