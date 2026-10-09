package com.soss;

import com.soss.controller.product.ProductForm;
import com.soss.model.*;
import com.soss.repository.*;
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
class Phase2CartCheckoutTest {

    @Autowired CartService carts;
    @Autowired CheckoutProducts checkoutProducts;
    @Autowired CheckoutOrders checkoutOrders;
    @Autowired CatalogueService catalogue;
    @Autowired OrderService orderService;
    @Autowired UserRepository users;
    @Autowired CartRepository cartRepository;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired InventoryRepository inventory;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired MockMvc mvc;

    private final String owner = "checkout@example.com";
    private Long customerId;
    private Long storeId;
    private Long secondStoreId;
    private Long productId;

    @BeforeEach
    void setup() {
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
    void staleOrEmptyCartCannotPlaceOrder() {
        var empty = carts.view(owner);
        assertThatThrownBy(() -> carts.place(owner, checkoutForm(empty.token())))
                .hasMessageContaining("empty");
        carts.change(owner, productId, 1);
        assertThatThrownBy(() -> carts.place(owner, checkoutForm(empty.token())))
                .hasMessageContaining("basket changed");
        assertThat(orders.count()).isZero();
    }



    @Test
    void promoCodesNormalizeReplaceRemoveAndRecalculate() {
        Long expensive = catalogue.save(null, productForm("Basket", "50.00", 10)).getId();
        carts.change(owner, expensive, 1);
        carts.promo(owner, "  save5  ");
        assertThat(carts.view(owner).promo()).isEqualTo("SAVE5");
        assertThat(carts.view(owner).discount()).isEqualByComparingTo("5.00");
        assertThat(carts.view(owner).total()).isEqualByComparingTo("48.00");
        String token = carts.view(owner).token();
        carts.promo(owner, "SAVE5");
        assertThat(carts.view(owner).token()).isEqualTo(token);
        assertThatThrownBy(() -> carts.promo(owner, "wrong"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(carts.view(owner).promo()).isEqualTo("SAVE5");
        carts.promo(owner, "freedel");
        assertThat(carts.view(owner).discount()).isEqualByComparingTo("3.00");
        assertThat(carts.view(owner).total()).isEqualByComparingTo("50.00");
        carts.promo(owner, "BULA10");
        carts.change(owner, expensive, 1);
        assertThat(carts.view(owner).discount()).isEqualByComparingTo("10.00");
        carts.promo(owner, "   ");
        assertThat(carts.view(owner).promo()).isNull();
        assertThat(carts.view(owner).total()).isEqualByComparingTo("103.00");
    }

    @Test
    void minimumSpendIsCheckedAtApplyAndCheckout() {
        carts.change(owner, productId, 1);
        assertThatThrownBy(() -> carts.promo(owner, "SAVE5"))
                .hasMessageContaining("50.00");
        Long expensive = catalogue.save(null, productForm("Basket", "50.00", 10)).getId();
        carts.change(owner, expensive, 1);
        carts.promo(owner, "SAVE5");
        carts.change(owner, expensive, -1);
        var basket = carts.view(owner);
        assertThat(basket.discount()).isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> carts.place(owner, checkoutForm(basket.token())))
                .hasMessageContaining("50.00");
        assertThat(orders.count()).isZero();
        assertThat(payments.count()).isZero();
        assertThat(carts.view(owner).token()).isEqualTo(basket.token());
    }

    @Test
    void directServiceRejectsMalformedCheckoutAndPreservesCart() {
        carts.change(owner, productId, 1);
        var basket = carts.view(owner);
        assertThatThrownBy(() -> carts.place(owner, null)).isInstanceOf(IllegalArgumentException.class);
        CheckoutForm form = checkoutForm("-".repeat(36));
        assertThatThrownBy(() -> carts.place(owner, form)).hasMessageContaining("token");
        form.setToken(basket.token());
        form.setAddress("   ");
        assertThatThrownBy(() -> carts.place(owner, form)).hasMessageContaining("address");
        form.setAddress("Test address");
        form.setDeliverySlot("2000-01-01 | 10am – 12pm");
        assertThatThrownBy(() -> carts.place(owner, form)).hasMessageContaining("delivery time");
        form.setDeliverySlot(carts.slots().getFirst());
        form.setPaymentMethod(null);
        assertThatThrownBy(() -> carts.place(owner, form)).hasMessageContaining("payment method");
        form.setPaymentMethod("untrusted");
        assertThatThrownBy(() -> carts.place(owner, form)).hasMessageContaining("payment method");
        assertThat(orders.count()).isZero();
        assertThat(payments.count()).isZero();
        assertThat(carts.view(owner).token()).isEqualTo(basket.token());
    }

    @Test
    void apiIgnoresSubmittedPricesAndRejectsInvalidFields() throws Exception {
        carts.change(owner, productId, 1);
        String token = carts.view(owner).token();
        mvc.perform(post("/api/checkout").with(user(owner).roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("""
                    {"token":"%s","address":" ","deliverySlot":"invalid","paymentMethod":"invalid"}
                    """.formatted(token))).andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();
        mvc.perform(post("/api/checkout").with(user(owner).roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("""
                    {"token":"%s","address":"Test address","deliverySlot":"%s",
                     "paymentMethod":"MyCash","total":0,"discount":999,"unitPrice":0}
                    """.formatted(token, carts.slots().getFirst()))).andExpect(status().isOk());
        assertThat(orders.findByCustomerIdAndCheckoutToken(customerId, token).orElseThrow()
                .getTotalAmount()).isEqualByComparingTo("7.50");
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
