package com.soss.model.delivery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeliveryStatusTest {

    @Test
    void stagesAreInTheOrderShownOnTheTrackingPage() {
        assertArrayEquals(
                new DeliveryStatus[]{
                        DeliveryStatus.CONFIRMED,
                        DeliveryStatus.PACKED,
                        DeliveryStatus.OUT_FOR_DELIVERY,
                        DeliveryStatus.DELIVERED},
                DeliveryStatus.values());
    }

    @Test
    void nextFollowsTheOrderAndEndsAtDelivered() {
        assertEquals(DeliveryStatus.PACKED, DeliveryStatus.CONFIRMED.next());
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.PACKED.next());
        assertEquals(DeliveryStatus.DELIVERED, DeliveryStatus.OUT_FOR_DELIVERY.next());
        assertNull(DeliveryStatus.DELIVERED.next());
    }
}
