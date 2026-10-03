package com.soss.controller.delivery;

import com.soss.model.delivery.Delivery;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats delivery times for display, e.g. "Today, 2pm – 4pm".
 * Used from the delivery templates as ${@deliveryDisplay.expectedWindow(delivery)}.
 */
@Component("deliveryDisplay")
public class DeliveryDisplay {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter UPDATED = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

    public String expectedWindow(Delivery delivery) {
        LocalDateTime from = delivery.getExpectedFrom();
        LocalDateTime to = delivery.getExpectedTo();
        if (from == null || to == null) {
            return "To be confirmed";
        }
        String day = from.toLocalDate().equals(LocalDate.now()) ? "Today" : DAY.format(from);
        return day + ", " + time(from.toLocalTime()) + " \u2013 " + time(to.toLocalTime());
    }

    public String updatedAt(Delivery delivery) {
        return UPDATED.format(delivery.getUpdatedAt()).replace("AM", "am").replace("PM", "pm");
    }

    private String time(LocalTime time) {
        String pattern = time.getMinute() == 0 ? "ha" : "h:mma";
        return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).format(time).toLowerCase(Locale.ENGLISH);
    }
}
