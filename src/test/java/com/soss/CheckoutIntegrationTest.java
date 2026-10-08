package com.soss;

import com.soss.controller.product.ProductForm;
import com.soss.model.*;
import com.soss.repository.*;
import com.soss.repository.delivery.DeliveryRepository;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.service.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:checkout-integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "inventory.local-login.enabled=false"
})
@AutoConfigureMockMvc
class CheckoutIntegrationTest {

    @Autowired CartService carts;
    @Autowired CheckoutProducts checkoutProducts;
    @Autowired CheckoutOrders checkoutOrders;
    @Autowired CatalogueService catalogue;
    @Autowired OrderService orderService;
    @Autowired OrderCancellationService cancellations;
    @Autowired UserRepository users;
    @Autowired CartRepository cartRepository;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired InventoryRepository inventory;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired DeliveryRepository deliveries;
    @Autowired MockMvc mvc;

    private final String owner = "checkout@example.com";
    private Long customerId;
    private Long storeId;
    private Long secondStoreId;
    private Long productId;

    @BeforeEach
    void setup() {
        deliveries.deleteAll();
        payments.deleteAll();
        orders.deleteAll();
        cartRepository.deleteAll();
        inventory.deleteAll();
        products.deleteAll();
        stores.deleteAll();
        users.deleteAll();
        customerId = customer(owner).getId();
        storeId = catalogue.addStore("Default branch").getId();
        secondStoreId = catalogue.addStore("Second branch").getId();
        productId = catalogue.save(null, productForm("Milk", "4.50", 10)).getId();
    }

    @Test
    void authenticatedCustomerGetsCartWithoutPreProvisioning() throws Exception {
        mvc.perform(get("/api/cart").with(user(owner).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        carts.view(owner);
        assertThat(cartRepository.count()).isEqualTo(1);
        assertThat(cartRepository.findByOwner(owner)).isPresent();
    }

    @Test
    void productSnapshotsUseDefaultStoreAndActiveStock() {
        var snapshot = checkoutProducts.find(productId).orElseThrow();
        assertThat(snapshot.id()).isEqualTo(productId);
        assertThat(snapshot.name()).isEqualTo("Milk");
        assertThat(snapshot.icon()).isEqualTo("milk");
        assertThat(snapshot.price()).isEqualByComparingTo("4.50");
        assertThat(snapshot.active()).isTrue();
        assertThat(catalogue.detail(productId, secondStoreId, true).getQuantity()).isZero();
        var form = catalogue.editForm(productId, storeId);
        form.setActive(false);
        catalogue.save(productId, form);
        assertThat(checkoutProducts.find(productId).orElseThrow().active()).isFalse();
        form = catalogue.editForm(productId, storeId);
        form.setActive(true);
        form.setQuantity(0);
        catalogue.save(productId, form);
        assertThat(checkoutProducts.find(productId).orElseThrow().active()).isFalse();
        assertThat(checkoutProducts.find(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void checkoutPaysFullTotalDeductsStockAndReplaysWithoutDuplicates() throws Exception {
        carts.change(owner, productId, 1);
        carts.change(owner, productId, 1);
        carts.promo(owner, "BULA10");
        var basket = carts.view(owner);
        CheckoutForm form = checkoutForm(basket.token());
        var receipt = carts.place(owner, form);
        Order order = orderService.getOrderById(receipt.orderId());
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getCheckoutToken()).isEqualTo(form.getToken());
        assertThat(order.getTotalAmount()).isEqualByComparingTo("11.10");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        var recorded = payments.findByOrderId(order.getId());
        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().getAmount()).isEqualByComparingTo(order.getTotalAmount());
        assertThat(recorded.getFirst().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(8);
        assertThat(catalogue.detail(productId, secondStoreId, true).getQuantity()).isZero();
        assertThat(carts.view(owner).lines()).isEmpty();
        assertThat(carts.place(owner, form)).isEqualTo(receipt);
        assertThat(orders.count()).isEqualTo(1);
        assertThat(payments.count()).isEqualTo(1);
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(8);
        mvc.perform(get("/orders/" + order.getId()).with(user(owner).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(11.10));
        mvc.perform(get("/payments/orders/" + order.getId()).with(user(owner).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void insufficientStockRollsBackOrderPaymentStockAndCart() {
        Long scarce = catalogue.save(null, productForm("Bread", "2.00", 1)).getId();
        carts.change(owner, productId, 1);
        carts.change(owner, scarce, 1);
        carts.change(owner, scarce, 1);
        var before = carts.view(owner);
        assertThatThrownBy(() -> carts.place(owner, checkoutForm(before.token())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("stock");
        assertThat(orders.count()).isZero();
        assertThat(payments.count()).isZero();
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(10);
        assertThat(catalogue.detail(scarce, storeId, true).getQuantity()).isEqualTo(1);
        assertThat(carts.view(owner).token()).isEqualTo(before.token());
        assertThat(carts.view(owner).lines()).hasSize(2);
    }

    @Test
    void checkoutControllerRedirectsToExistingConfirmationPage() throws Exception {
        carts.change(owner, productId, 1);
        CheckoutForm form = checkoutForm(carts.view(owner).token());
        var result = mvc.perform(post("/checkout").with(user(owner).roles("CUSTOMER")).with(csrf())
                .param("token", form.getToken()).param("address", form.getAddress())
                .param("deliverySlot", form.getDeliverySlot()).param("paymentMethod", form.getPaymentMethod()))
                .andExpect(status().is3xxRedirection()).andReturn();
        Order order = orders.findByCustomerIdAndCheckoutToken(customerId, form.getToken()).orElseThrow();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/orders/" + order.getId() + "/confirmation");
        mvc.perform(get(result.getResponse().getRedirectedUrl()).with(user(owner).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(view().name("confirmation"));
    }

    @Test
    void sameTokenIsScopedToCustomer() {
        String token = UUID.randomUUID().toString();
        var first = checkoutOrders.place(request(owner, token));
        String otherOwner = "other@example.com";
        customer(otherOwner);
        assertThat(checkoutOrders.findExisting(otherOwner, token)).isEmpty();
        var second = checkoutOrders.place(request(otherOwner, token));
        assertThat(first.orderId()).isNotEqualTo(second.orderId());
        assertThat(checkoutOrders.findExisting(owner, token)).contains(first);
        assertThat(checkoutOrders.findExisting(otherOwner, token)).contains(second);
    }

    @Test
    void concurrentPlacementCreatesOnlyOneOrderPaymentAndStockDeduction() throws Exception {
        var request = request(owner, UUID.randomUUID().toString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<CheckoutOrders.Receipt> placement = () -> checkoutOrders.place(request);
            var results = executor.invokeAll(List.of(placement, placement));
            assertThat(results.get(0).get()).isEqualTo(results.get(1).get());
        }
        assertThat(orders.count()).isEqualTo(1);
        assertThat(payments.count()).isEqualTo(1);
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(9);
    }

    @Test
    void concurrentFirstVisitsProvisionOnlyOneCart() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<CartService.Basket> visit = () -> carts.view(owner);
            var results = executor.invokeAll(List.of(visit, visit));
            assertThat(results.get(0).get().token()).isEqualTo(results.get(1).get().token());
        }
        assertThat(cartRepository.count()).isEqualTo(1);
    }

    @Test
    void cartsSupportEmailsAcceptedByAuthentication() {
        String longEmail = "a".repeat(110) + "@example.com";
        customer(longEmail);
        assertThat(carts.view(longEmail).lines()).isEmpty();
        assertThat(cartRepository.findByOwner(longEmail)).isPresent();
    }

    @Test
    void cancellingPaidCheckoutRestoresStockCancelsDeliveryAndRecordsOneRefund() {
        carts.change(owner, productId, 1);
        var receipt = carts.place(owner, checkoutForm(carts.view(owner).token()));
        Order order = orderService.getOrderById(receipt.orderId());
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(9);

        cancellations.cancel(order.getId(), customerId);

        assertThat(orderService.getOrderById(order.getId()).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(10);
        assertThat(deliveries.findByOrderId(order.getId())).isEmpty();
        assertThat(payments.findByOrderId(order.getId()))
                .extracting(Payment::getStatus)
                .containsExactlyInAnyOrder(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);

        assertThatThrownBy(() -> cancellations.cancel(order.getId(), customerId))
                .isInstanceOf(IllegalStateException.class);
        assertThat(payments.findByOrderId(order.getId())).hasSize(2);
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(10);
    }

    @Test
    void cancellationRefusesAnOrderWhoseDeliveryIsAlreadyOutForDelivery() {
        carts.change(owner, productId, 1);
        var receipt = carts.place(owner, checkoutForm(carts.view(owner).token()));
        Order order = orderService.getOrderById(receipt.orderId());
        var delivery = deliveries.findByOrderId(order.getId()).orElseThrow();
        delivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        deliveries.saveAndFlush(delivery);

        assertThatThrownBy(() -> cancellations.cancel(order.getId(), customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("delivery is in progress");
        assertThat(orderService.getOrderById(order.getId()).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(9);
        assertThat(payments.findByOrderId(order.getId())).hasSize(1);
    }

    private User customer(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("Checkout customer");
        user.setPhone("9000000");
        user.setPasswordHash("test-only-password-hash");
        user.setRole(Role.CUSTOMER);
        return users.saveAndFlush(user);
    }

    private ProductForm productForm(String name, String price, int quantity) {
        ProductForm form = new ProductForm();
        form.setName(name);
        form.setCategory("Groceries");
        form.setDescription(name);
        form.setPrice(new BigDecimal(price));
        form.setIcon("milk");
        form.setActive(true);
        form.setStoreId(storeId);
        form.setQuantity(quantity);
        return form;
    }

    private CheckoutForm checkoutForm(String token) {
        CheckoutForm form = new CheckoutForm();
        form.setToken(token);
        form.setAddress("Test address");
        form.setDeliverySlot(carts.slots().getFirst());
        form.setPaymentMethod("M-PAiSA");
        return form;
    }

    private CheckoutOrders.Request request(String email, String token) {
        return new CheckoutOrders.Request(email, token, "Test address", carts.slots().getFirst(),
                "M-PAiSA", "FJD", new BigDecimal("4.50"), new BigDecimal("3.00"),
                BigDecimal.ZERO, new BigDecimal("7.50"), null,
                List.of(new CheckoutOrders.Item(productId, "Milk", 1,
                        new BigDecimal("4.50"), new BigDecimal("4.50"))));
    }
}
