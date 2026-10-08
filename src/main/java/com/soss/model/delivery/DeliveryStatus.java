package com.soss.model.delivery;

/**
 * Stages of a grocery delivery, in the order shown on the "Track your grocery delivery" page.
 * The label and description texts come from the Bula Basket interface design.
 */
public enum DeliveryStatus {

    CONFIRMED("Confirmed", "Your order has been confirmed."),
    PACKED("Packed", "Your groceries have been packed by store staff."),
    OUT_FOR_DELIVERY("Out for delivery", "Your driver is on the way."),
    DELIVERED("Delivered", "Order will be marked delivered after handover."),
    CANCELLED("Cancelled", "This delivery was cancelled with the order.");

    private final String label;
    private final String description;

    DeliveryStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** The stage that follows this one, or null when the delivery is already complete. */
    public DeliveryStatus next() {
        int nextIndex = ordinal() + 1;
        return nextIndex < values().length ? values()[nextIndex] : null;
    }
}
