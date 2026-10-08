package com.soss;

import com.soss.controller.product.ProductForm;
import com.soss.model.Cart;
import com.soss.model.CheckoutForm;
import com.soss.model.OrderStatus;
import com.soss.model.Role;
import com.soss.model.User;
import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.repository.CartRepository;
import com.soss.repository.InventoryRepository;
import com.soss.repository.OrderRepository;
import com.soss.repository.PaymentRepository;
import com.soss.repository.ProductRepository;
import com.soss.repository.StoreRepository;
import com.soss.repository.UserRepository;
import com.soss.repository.delivery.DeliveryRepository;
import com.soss.service.CartService;
import com.soss.service.CatalogueService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 2 delivery flow: a customer checks out, staff move the delivery through its stages,
 * and the customer confirms receiving the order, which completes the order.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:delivery-confirmation;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class DeliveryConfirmationFlowTest {

    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String OTHER_CUSTOMER_EMAIL = "other@example.com";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CartService carts;
    @Autowired
    private CatalogueService catalogue;
    @Autowired
    private UserRepository users;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private PaymentRepository payments;
    @Autowired
    private DeliveryRepository deliveries;
    @Autowired
    private InventoryRepository inventory;
    @Autowired
    private ProductRepository products;
    @Autowired
    private StoreRepository stores;

    private Long customerId;
    private Long driverId;
    private Long productId;

    @BeforeEach
    void setUp() {
        deliveries.deleteAll();
        payments.deleteAll();
        orders.deleteAll();
        cartRepository.deleteAll();
        inventory.deleteAll();
        products.deleteAll();
        stores.deleteAll();
        users.deleteAll();

        customerId = customer(CUSTOMER_EMAIL).getId();
        customer(OTHER_CUSTOMER_EMAIL);
        driverId = account("driver@example.com", "Driver One", Role.DELIVERY).getId();
        Long storeId = catalogue.addStore("Suva").getId();

        ProductForm form = new ProductForm();
        form.setName("Fresh Milk");
        form.setCategory("Dairy");
        form.setDescription("Fresh milk");
        form.setPrice(new BigDecimal("4.50"));
        form.setIcon("milk");
        form.setActive(true);
        form.setStoreId(storeId);
        form.setQuantity(8);
        productId = catalogue.save(null, form).getId();
    }

    @Test
    void customerConfirmationCompletesTheOrderAfterStaffDeliverIt() throws Exception {
        Long orderId = placeOrder();
        Delivery delivery = deliveries.findByOrderId(orderId).orElseThrow();
        UserRequestPostProcessor staff = user("staff").roles("STAFF");
        UserRequestPostProcessor customer = customerUser(CUSTOMER_EMAIL);

        // Only a delivery driver can be assigned, so a customer's ID is refused.
        mvc.perform(post("/delivery/" + delivery.getId() + "/driver").with(staff).with(csrf())
                        .param("staffId", customerId.toString()))
                .andExpect(redirectedUrl("/delivery/manage"));
        assertNull(reload(orderId).getAssignedStaffId());
        mvc.perform(get("/delivery/manage").with(staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Driver One")));

        // Going out for delivery needs a driver, so this is refused and the status stays Packed.
        mvc.perform(post("/delivery/" + delivery.getId() + "/status").with(staff).with(csrf())
                        .param("status", "PACKED"))
                .andExpect(redirectedUrl("/delivery/manage"));
        mvc.perform(post("/delivery/" + delivery.getId() + "/status").with(staff).with(csrf())
                        .param("status", "OUT_FOR_DELIVERY"))
                .andExpect(redirectedUrl("/delivery/manage"));
        assertEquals(DeliveryStatus.PACKED, reload(orderId).getStatus());

        mvc.perform(post("/delivery/" + delivery.getId() + "/driver").with(staff).with(csrf())
                        .param("staffId", driverId.toString()))
                .andExpect(redirectedUrl("/delivery/manage"));
        mvc.perform(post("/delivery/" + delivery.getId() + "/status").with(staff).with(csrf())
                        .param("status", "OUT_FOR_DELIVERY"))
                .andExpect(redirectedUrl("/delivery/manage"));
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, reload(orderId).getStatus());

        // The customer cannot confirm before the order has been delivered.
        mvc.perform(post("/delivery/track/" + orderId + "/confirm").with(customer).with(csrf()))
                .andExpect(redirectedUrl("/delivery/track/" + orderId));
        assertNull(reload(orderId).getCustomerConfirmedAt());
        assertEquals(OrderStatus.PAID, orders.findById(orderId).orElseThrow().getStatus());

        // Marking it delivered needs the name of the person who received it.
        mvc.perform(post("/delivery/" + delivery.getId() + "/status").with(staff).with(csrf())
                        .param("status", "DELIVERED"))
                .andExpect(redirectedUrl("/delivery/manage"));
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, reload(orderId).getStatus());
        mvc.perform(post("/delivery/" + delivery.getId() + "/status").with(staff).with(csrf())
                        .param("status", "DELIVERED")
                        .param("receivedBy", "Mere Tui"))
                .andExpect(redirectedUrl("/delivery/manage"));
        Delivery delivered = reload(orderId);
        assertEquals(DeliveryStatus.DELIVERED, delivered.getStatus());
        assertEquals("Mere Tui", delivered.getReceivedBy());
        assertNotNull(delivered.getDeliveredAt());

        // Another customer cannot confirm this order.
        mvc.perform(post("/delivery/track/" + orderId + "/confirm")
                        .with(customerUser(OTHER_CUSTOMER_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());
        assertNull(reload(orderId).getCustomerConfirmedAt());

        // The tracking page offers the confirm button until the customer has used it.
        mvc.perform(get("/delivery/track/" + orderId).with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirm I received my order")))
                .andExpect(content().string(containsString("Mere Tui")))
                .andExpect(content().string(containsString("Driver One")))
                .andExpect(content().string(containsString("Your order has been handed over.")));

        mvc.perform(post("/delivery/track/" + orderId + "/confirm").with(customer).with(csrf()))
                .andExpect(redirectedUrl("/delivery/track/" + orderId));

        assertNotNull(reload(orderId).getCustomerConfirmedAt());
        assertEquals(OrderStatus.COMPLETED, orders.findById(orderId).orElseThrow().getStatus());
        mvc.perform(get("/delivery/track/" + orderId).with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Confirm I received my order"))));
    }

    @Test
    void failedAttemptSendsTheDeliveryBackToPackedWithTheReason() throws Exception {
        Long orderId = placeOrder();
        Delivery delivery = deliveries.findByOrderId(orderId).orElseThrow();
        UserRequestPostProcessor staff = user("staff").roles("STAFF");

        mvc.perform(post("/delivery/" + delivery.getId() + "/driver").with(staff).with(csrf())
                        .param("staffId", driverId.toString()));
        mvc.perform(post("/delivery/" + delivery.getId() + "/status").with(staff).with(csrf())
                        .param("status", "PACKED"));
        mvc.perform(post("/delivery/" + delivery.getId() + "/status").with(staff).with(csrf())
                        .param("status", "OUT_FOR_DELIVERY"));
        mvc.perform(post("/delivery/" + delivery.getId() + "/failed").with(staff).with(csrf())
                        .param("reason", "Nobody home"))
                .andExpect(redirectedUrl("/delivery/manage"));

        Delivery after = reload(orderId);
        assertEquals(DeliveryStatus.PACKED, after.getStatus());
        assertEquals("Nobody home", after.getFailedAttemptReason());

        mvc.perform(get("/delivery/track/" + orderId).with(customerUser(CUSTOMER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nobody home")));
    }

    private Long placeOrder() {
        carts.change(CUSTOMER_EMAIL, productId, 1);
        Cart cart = cartRepository.findByOwner(CUSTOMER_EMAIL).orElseThrow();
        carts.place(CUSTOMER_EMAIL, checkoutForm(cart));
        return orders.findByCustomerIdOrderByOrderDateDesc(customerId).getFirst().getId();
    }

    private Delivery reload(Long orderId) {
        return deliveries.findByOrderId(orderId).orElseThrow();
    }

    private CheckoutForm checkoutForm(Cart cart) {
        CheckoutForm form = new CheckoutForm();
        form.setToken(cart.checkoutToken);
        form.setAddress("1 Victoria Parade, Suva");
        form.setDeliverySlot(carts.slots().getFirst());
        form.setPaymentMethod("M-PAiSA");
        return form;
    }

    private User customer(String email) {
        return account(email, "Customer", Role.CUSTOMER);
    }

    private User account(String email, String name, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone("9000000");
        user.setPasswordHash("unused");
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private UserRequestPostProcessor customerUser(String email) {
        return user(email).roles("CUSTOMER");
    }
}
