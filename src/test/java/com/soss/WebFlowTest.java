package com.soss;

import com.soss.controller.product.ProductForm;
import com.soss.model.Cart;
import com.soss.model.CheckoutForm;
import com.soss.model.Product;
import com.soss.model.Role;
import com.soss.model.User;
import com.soss.model.delivery.Delivery;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:web-flow;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class WebFlowTest {

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
    private Long productId;
    private Long storeId;

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
        storeId = catalogue.addStore("Suva").getId();

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
    void publicPagesRenderAndGuestShoppingRoutesRedirectToLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"site-search\"")))
                .andExpect(content().string(containsString("action=\"/products\"")));
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Log in")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"q\""))));
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"q\""))));
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fresh Milk")));
        mvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/inventory"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/delivery/manage"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void sharedHeaderUsesRealRoutesAndHidesCustomerLinksFromGuests() throws Exception {
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/images/bula-basket-logo.png")))
                .andExpect(content().string(containsString("href=\"/\"")))
                .andExpect(content().string(containsString("href=\"/products\"")))
                .andExpect(content().string(containsString("action=\"/products\"")))
                .andExpect(content().string(containsString("id=\"catalogue-query\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"site-search\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/register\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/search\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/cart\""))));

        UserRequestPostProcessor customer = customerUser(CUSTOMER_EMAIL);
        mvc.perform(get("/products/" + productId).param("storeId", storeId.toString()).with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"site-search\""))))
                .andExpect(content().string(containsString("href=\"/cart\"")))
                .andExpect(content().string(containsString("href=\"/orders\"")))
                .andExpect(content().string(containsString("href=\"/profile\"")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/search\""))));
    }

    @Test
    void productsSearchCartAndCheckoutUseTheExpectedCustomerFlow() throws Exception {
        UserRequestPostProcessor customer = customerUser(CUSTOMER_EMAIL);

        mvc.perform(get("/products").param("q", "milk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fresh Milk")));

        mvc.perform(post("/cart/quantity").with(customer).with(csrf())
                        .param("productId", productId.toString()).param("delta", "1")
                        .param("returnTo", "/products?storeId=" + storeId))
                .andExpect(redirectedUrl("/products?storeId=" + storeId));
        mvc.perform(get("/cart").with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proceed to checkout")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Delivery details"))));
        mvc.perform(get("/checkout").with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"site-search\""))))
                .andExpect(content().string(containsString("Delivery details")))
                .andExpect(content().string(containsString("Pay with")));
    }

    @Test
    void nonCustomersCannotSeeShoppingActionsAndMissingDeliveryUsesStyledTrackingPage() throws Exception {
        mvc.perform(get("/products/" + productId).param("storeId", storeId.toString())
                        .with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Add to cart"))));
        mvc.perform(get("/cart").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));

        carts.change(CUSTOMER_EMAIL, productId, 1);
        Cart cart = cartRepository.findByOwner(CUSTOMER_EMAIL).orElseThrow();
        carts.place(CUSTOMER_EMAIL, checkoutForm(cart));
        Long orderId = orders.findByCustomerIdOrderByOrderDateDesc(customerId).getFirst().getId();
        deliveries.deleteAll();

        mvc.perform(get("/delivery/track/" + orderId).with(customerUser(CUSTOMER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Delivery tracking is not available for this order yet.")));
    }

    @Test
    void customerCanCheckoutThenViewOwnOrderConfirmationAndDelivery() throws Exception {
        UserRequestPostProcessor customer = customerUser(CUSTOMER_EMAIL);

        mvc.perform(post("/cart/quantity").with(customer).with(csrf())
                        .param("productId", productId.toString())
                        .param("delta", "1"))
                .andExpect(redirectedUrl("/cart"));
        mvc.perform(get("/cart").with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fresh Milk")));

        Cart cart = cartRepository.findByOwner(CUSTOMER_EMAIL).orElseThrow();
        mvc.perform(post("/checkout").with(customer).with(csrf())
                        .param("token", cart.checkoutToken)
                        .param("address", "1 Victoria Parade, Suva")
                        .param("deliverySlot", carts.slots().getFirst())
                        .param("paymentMethod", "M-PAiSA"))
                .andExpect(status().is3xxRedirection());

        var order = orders.findByCustomerIdOrderByOrderDateDesc(customerId).getFirst();
        String confirmation = "/orders/" + order.getId() + "/confirmation";
        String tracking = "/delivery/track/" + order.getId();

        mvc.perform(get(confirmation).with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Order #" + order.getId())))
                .andExpect(content().string(containsString(">Orders<")));
        mvc.perform(get("/orders").with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(tracking)));
        mvc.perform(get("/delivery/track").with(customer)
                        .param("orderId", order.getId().toString()))
                .andExpect(redirectedUrl(tracking));
        mvc.perform(get(tracking).with(customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Track your grocery delivery")));

        assertEquals(7, catalogue.detail(productId, catalogue.store(null).getId(), true).getQuantity());
        assertEquals(1, payments.count());
        assertEquals(1, deliveries.count());
    }

    @Test
    void customerCannotOpenAnotherCustomersOrderOrDelivery() throws Exception {
        carts.change(CUSTOMER_EMAIL, productId, 1);
        Cart cart = cartRepository.findByOwner(CUSTOMER_EMAIL).orElseThrow();
        carts.place(CUSTOMER_EMAIL, checkoutForm(cart));
        Long orderId = orders.findByCustomerIdOrderByOrderDateDesc(customerId).getFirst().getId();

        mvc.perform(get("/orders/" + orderId + "/confirmation")
                        .with(customerUser(OTHER_CUSTOMER_EMAIL)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/delivery/track/" + orderId)
                        .with(customerUser(OTHER_CUSTOMER_EMAIL)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/orders/customer/" + customerId + "/view")
                        .with(customerUser(OTHER_CUSTOMER_EMAIL)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/orders/" + orderId + "/cancel")
                        .with(customerUser(OTHER_CUSTOMER_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/orders/" + orderId + "/confirmation").with(customerUser(CUSTOMER_EMAIL)))
                .andExpect(status().isOk());
        mvc.perform(get("/payment-history").with(customerUser(OTHER_CUSTOMER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Order #" + orderId))));
    }

    @Test
    void orderErrorsUseHtmlForPagesAndJsonForApiRequests() throws Exception {
        UserRequestPostProcessor customer = customerUser(CUSTOMER_EMAIL);

        mvc.perform(get("/orders/999999/confirmation").with(customer)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
        mvc.perform(get("/orders/999999").with(customer)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void staffAndDeliveryRolesOnlySeeTheirAllowedManagementPages() throws Exception {
        deliveries.saveAndFlush(new Delivery(99L, "Suva", null, null));
        mvc.perform(get("/inventory").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
        mvc.perform(get("/inventory").with(user("delivery").roles("DELIVERY")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/delivery/manage").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
        mvc.perform(get("/delivery/manage").with(user("delivery").roles("DELIVERY")))
                .andExpect(status().isOk());
        mvc.perform(get("/delivery/manage").with(user("staff").roles("STAFF")))
                .andExpect(content().string(containsString("Expected time")))
                .andExpect(content().string(containsString("Driver")));
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
        User user = new User();
        user.setFullName("Customer");
        user.setEmail(email);
        user.setPhone("9000000");
        user.setPasswordHash("unused");
        user.setRole(Role.CUSTOMER);
        return users.saveAndFlush(user);
    }

    private UserRequestPostProcessor customerUser(String email) {
        return user(email).roles("CUSTOMER");
    }
}
