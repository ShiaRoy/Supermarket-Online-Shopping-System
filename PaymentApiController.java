package com.soss.checkout;

import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PaymentApiController {
    private final PaymentService payments;
    public PaymentApiController(PaymentService payments) { this.payments = payments; }

    @GetMapping("/payment-methods")
    public List<PaymentService.MethodView> methods() { return payments.methods(); }

    @PostMapping("/orders/{id}/payment")
    public PaymentService.PaymentView start(@PathVariable Long id, Principal user) {
        return payments.start(id, user.getName());
    }

    @GetMapping("/orders/{id}/payment")
    public PaymentService.PaymentView status(@PathVariable Long id, Principal user) {
        return payments.status(id, user.getName());
    }
}
