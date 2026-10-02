package com.soss.controller.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InventoryLoginController {
    @GetMapping("/inventory/login")
    String login() { return "products/login"; }
}
