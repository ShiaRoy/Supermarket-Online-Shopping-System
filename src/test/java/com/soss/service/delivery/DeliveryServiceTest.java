package com.soss.service.delivery;

import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.repository.delivery.DeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 2, 14, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 10, 2, 16, 0);

    @Mock
    private DeliveryRepository repository;

    @InjectMocks
    private DeliveryService service;

    private Delivery deliveryWithStatus(DeliveryStatus status) {
        Delivery delivery = new Delivery(1L, "Vuci Road, Nausori", FROM, TO);
        delivery.setStatus(status);
        return delivery;
    }

    // ---- createForOrder ----

    @Test
    void createForOrder_savesNewDeliveryAsConfirmed() {
        when(repository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery created = service.createForOrder(1L, "Vuci Road, Nausori", FROM, TO);

        assertEquals(1L, created.getOrderId());
        assertEquals("Vuci Road, Nausori", created.getDeliveryAddress());
        assertEquals(DeliveryStatus.CONFIRMED, created.getStatus());
        verify(repository).save(any(Delivery.class));
    }

    @Test
    void createForOrder_rejectsSecondDeliveryForSameOrder() {
        when(repository.findByOrderId(1L)).thenReturn(Optional.of(deliveryWithStatus(DeliveryStatus.CONFIRMED)));

        assertThrows(IllegalStateException.class,
                () -> service.createForOrder(1L, "Vuci Road, Nausori", FROM, TO));
        verify(repository, never()).save(any(Delivery.class));
    }

    @Test
    void createForOrder_rejectsWindowThatEndsBeforeItStarts() {
        when(repository.findByOrderId(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.createForOrder(1L, "Vuci Road, Nausori", TO, FROM));
        verify(repository, never()).save(any(Delivery.class));
    }

    @Test
    void createForOrder_rejectsBlankAddress() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createForOrder(1L, "   ", FROM, TO));
        verify(repository, never()).save(any(Delivery.class));
    }

    // ---- advanceStatus ----

    @Test
    void advanceStatus_movesToTheNextStage() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.advanceStatus(5L, DeliveryStatus.PACKED);

        assertEquals(DeliveryStatus.PACKED, delivery.getStatus());
    }

    @Test
    void advanceStatus_rejectsSkippingAStage() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.DELIVERED));
        assertEquals(DeliveryStatus.CONFIRMED, delivery.getStatus());
    }

    @Test
    void advanceStatus_rejectsGoingBackwards() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PACKED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.CONFIRMED));
        assertEquals(DeliveryStatus.PACKED, delivery.getStatus());
    }

    @Test
    void advanceStatus_rejectsAnyChangeAfterDelivered() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.DELIVERED));
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
    }

    @Test
    void advanceStatus_failsForUnknownDelivery() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.advanceStatus(99L, DeliveryStatus.PACKED));
    }

    // ---- assignStaff ----

    @Test
    void assignStaff_recordsTheStaffMember() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.assignStaff(5L, 42L);

        assertEquals(42L, delivery.getAssignedStaffId());
    }

    // ---- findAll ----

    @Test
    void findAll_withStatusReturnsOnlyThatStage() {
        Delivery packed = deliveryWithStatus(DeliveryStatus.PACKED);
        when(repository.findByStatusOrderByUpdatedAtDesc(DeliveryStatus.PACKED)).thenReturn(List.of(packed));

        List<Delivery> result = service.findAll(DeliveryStatus.PACKED);

        assertEquals(1, result.size());
        assertEquals(DeliveryStatus.PACKED, result.get(0).getStatus());
    }

    @Test
    void findAll_withoutStatusReturnsEverything() {
        when(repository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(
                deliveryWithStatus(DeliveryStatus.CONFIRMED),
                deliveryWithStatus(DeliveryStatus.DELIVERED)));

        assertEquals(2, service.findAll(null).size());
    }
}
