package com.soss.service.delivery;

import com.soss.model.Role;
import com.soss.model.User;
import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.repository.UserRepository;
import com.soss.repository.delivery.DeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeliveryService service;

    private Delivery deliveryWithStatus(DeliveryStatus status) {
        Delivery delivery = new Delivery(1L, "Vuci Road, Nausori", FROM, TO);
        delivery.setStatus(status);
        return delivery;
    }


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

    

    @Test
    void advanceStatus_movesToTheNextStageAndRecordsTheTime() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.advanceStatus(5L, DeliveryStatus.PACKED, null);

        assertEquals(DeliveryStatus.PACKED, delivery.getStatus());
        assertNotNull(delivery.getPackedAt());
    }

    @Test
    void advanceStatus_rejectsSkippingAStage() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.DELIVERED, "Mere Tui"));
        assertEquals(DeliveryStatus.CONFIRMED, delivery.getStatus());
    }

    @Test
    void advanceStatus_rejectsGoingBackwards() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PACKED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.CONFIRMED, null));
        assertEquals(DeliveryStatus.PACKED, delivery.getStatus());
    }

    @Test
    void advanceStatus_rejectsAnyChangeAfterDelivered() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.DELIVERED, "Mere Tui"));
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
    }

    @Test
    void advanceStatus_failsForUnknownDelivery() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.advanceStatus(99L, DeliveryStatus.PACKED, null));
    }

    @Test
    void advanceStatus_needsADriverBeforeGoingOutForDelivery() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PACKED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.OUT_FOR_DELIVERY, null));
        assertEquals(DeliveryStatus.PACKED, delivery.getStatus());
        assertNull(delivery.getOutForDeliveryAt());
    }

    @Test
    void advanceStatus_goesOutForDeliveryOnceADriverIsAssigned() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PACKED);
        delivery.setAssignedStaffId(42L);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.advanceStatus(5L, DeliveryStatus.OUT_FOR_DELIVERY, null);

        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, delivery.getStatus());
        assertNotNull(delivery.getOutForDeliveryAt());
    }

    @Test
    void advanceStatus_sendingOutAgainClearsTheOldFailedAttempt() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PACKED);
        delivery.setAssignedStaffId(42L);
        delivery.setFailedAttemptReason("Nobody home");
        delivery.setFailedAttemptAt(LocalDateTime.now());
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.advanceStatus(5L, DeliveryStatus.OUT_FOR_DELIVERY, null);

        assertNull(delivery.getFailedAttemptReason());
        assertNull(delivery.getFailedAttemptAt());
    }

    @Test
    void advanceStatus_deliveredNeedsToKnowWhoReceivedIt() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.DELIVERED, null));
        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.DELIVERED, "   "));
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, delivery.getStatus());
        assertNull(delivery.getReceivedBy());
    }

    @Test
    void advanceStatus_deliveredRecordsWhoReceivedItAndWhen() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.advanceStatus(5L, DeliveryStatus.DELIVERED, "  Mere Tui  ");

        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
        assertEquals("Mere Tui", delivery.getReceivedBy());
        assertNotNull(delivery.getDeliveredAt());
    }

    @Test
    void advanceStatus_rejectsACancelledDelivery() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        delivery.setCancelledAt(LocalDateTime.now());
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.advanceStatus(5L, DeliveryStatus.PACKED, null));
        assertEquals(DeliveryStatus.CONFIRMED, delivery.getStatus());
    }

   

    @Test
    void reportFailedAttempt_sendsTheDeliveryBackToPackedWithTheReason() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        delivery.setOutForDeliveryAt(LocalDateTime.now());
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.reportFailedAttempt(5L, "  Nobody home  ");

        assertEquals(DeliveryStatus.PACKED, delivery.getStatus());
        assertEquals("Nobody home", delivery.getFailedAttemptReason());
        assertNotNull(delivery.getFailedAttemptAt());
        assertNull(delivery.getOutForDeliveryAt());
    }

    @Test
    void reportFailedAttempt_onlyWorksWhileOutForDelivery() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class,
                () -> service.reportFailedAttempt(5L, "Nobody home"));
        assertEquals(DeliveryStatus.CONFIRMED, delivery.getStatus());
    }

    @Test
    void reportFailedAttempt_needsAReason() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalArgumentException.class,
                () -> service.reportFailedAttempt(5L, "   "));
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, delivery.getStatus());
    }

    

    @Test
    void confirmReceived_recordsWhenTheCustomerConfirmed() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        when(repository.findByOrderId(1L)).thenReturn(Optional.of(delivery));

        service.confirmReceived(1L);

        assertNotNull(delivery.getCustomerConfirmedAt());
    }

    @Test
    void confirmReceived_isRefusedBeforeTheOrderIsDelivered() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        when(repository.findByOrderId(1L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class, () -> service.confirmReceived(1L));
        assertNull(delivery.getCustomerConfirmedAt());
    }

    @Test
    void confirmReceived_keepsTheFirstConfirmationTime() {
        LocalDateTime first = LocalDateTime.of(2026, 10, 3, 9, 0);
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        delivery.setCustomerConfirmedAt(first);
        when(repository.findByOrderId(1L)).thenReturn(Optional.of(delivery));

        service.confirmReceived(1L);

        assertEquals(first, delivery.getCustomerConfirmedAt());
    }

    @Test
    void confirmReceived_failsWhenTheOrderHasNoDelivery() {
        when(repository.findByOrderId(7L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.confirmReceived(7L));
    }

    

    @Test
    void cancelForOrder_marksAnEarlyDeliveryAsCancelled() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PACKED);
        when(repository.findByOrderId(1L)).thenReturn(Optional.of(delivery));

        Optional<Delivery> result = service.cancelForOrder(1L);

        assertTrue(result.isPresent());
        assertNotNull(delivery.getCancelledAt());
    }

    @Test
    void cancelForOrder_isRefusedOnceTheDeliveryIsOnItsWay() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        when(repository.findByOrderId(1L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class, () -> service.cancelForOrder(1L));
        assertNull(delivery.getCancelledAt());
    }

    @Test
    void cancelForOrder_doesNothingWhenTheOrderHasNoDelivery() {
        when(repository.findByOrderId(7L)).thenReturn(Optional.empty());

        assertTrue(service.cancelForOrder(7L).isEmpty());
    }

    

    private User userWithRole(Long id, String name, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setRole(role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void assignStaff_recordsTheDeliveryDriver() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));
        when(userRepository.findById(42L)).thenReturn(Optional.of(userWithRole(42L, "Sefa Naidu", Role.DELIVERY)));

        service.assignStaff(5L, 42L);

        assertEquals(42L, delivery.getAssignedStaffId());
    }

    @Test
    void assignStaff_rejectsSomeoneWhoIsNotADeliveryDriver() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));
        when(userRepository.findById(8L)).thenReturn(Optional.of(userWithRole(8L, "A Customer", Role.CUSTOMER)));

        assertThrows(IllegalArgumentException.class, () -> service.assignStaff(5L, 8L));
        assertNull(delivery.getAssignedStaffId());
    }

    @Test
    void assignStaff_rejectsAnIdThatDoesNotExist() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.assignStaff(5L, 7L));
        assertNull(delivery.getAssignedStaffId());
    }

    @Test
    void assignStaff_canRemoveTheDriverBeforeTheDeliveryGoesOut() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PACKED);
        delivery.setAssignedStaffId(42L);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        service.assignStaff(5L, null);

        assertNull(delivery.getAssignedStaffId());
    }

    @Test
    void assignStaff_cannotRemoveTheDriverOnceOutForDelivery() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        delivery.setAssignedStaffId(42L);
        when(repository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class, () -> service.assignStaff(5L, null));
        assertEquals(42L, delivery.getAssignedStaffId());
    }

    @Test
    void assignStaff_isRefusedForCancelledAndDeliveredDeliveries() {
        Delivery cancelled = deliveryWithStatus(DeliveryStatus.CONFIRMED);
        cancelled.setCancelledAt(LocalDateTime.now());
        Delivery delivered = deliveryWithStatus(DeliveryStatus.DELIVERED);
        when(repository.findById(5L)).thenReturn(Optional.of(cancelled));
        when(repository.findById(6L)).thenReturn(Optional.of(delivered));

        assertThrows(IllegalStateException.class, () -> service.assignStaff(5L, 42L));
        assertThrows(IllegalStateException.class, () -> service.assignStaff(6L, 42L));
    }

   

    @Test
    void findDrivers_returnsOnlyDeliveryAccountsSortedByName() {
        when(userRepository.findAll()).thenReturn(List.of(
                userWithRole(1L, "Zara Ali", Role.DELIVERY),
                userWithRole(2L, "A Customer", Role.CUSTOMER),
                userWithRole(3L, "mere tui", Role.DELIVERY),
                userWithRole(4L, "Some Staff", Role.STAFF)));

        List<User> drivers = service.findDrivers();

        assertEquals(2, drivers.size());
        assertEquals("mere tui", drivers.get(0).getFullName());
        assertEquals("Zara Ali", drivers.get(1).getFullName());
    }

    @Test
    void driverNames_mapsEachDriverIdToTheirName() {
        when(userRepository.findAll()).thenReturn(List.of(
                userWithRole(3L, "Mere Tui", Role.DELIVERY),
                userWithRole(4L, "Some Staff", Role.STAFF)));

        Map<Long, String> names = service.driverNames();

        assertEquals(1, names.size());
        assertEquals("Mere Tui", names.get(3L));
    }

    

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
