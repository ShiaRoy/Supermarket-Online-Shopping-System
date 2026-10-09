package com.soss.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Delivery choices shared by the cart and order integration boundary. */
final class CartSlots {
    List<String> available() {
        LocalDate today = LocalDate.now(ZoneId.of("Pacific/Fiji"));
        return List.of(today.plusDays(1) + " | 10am – 12pm",
                today.plusDays(1) + " | 2pm – 4pm",
                today.plusDays(2) + " | 2pm – 4pm");
    }
}
