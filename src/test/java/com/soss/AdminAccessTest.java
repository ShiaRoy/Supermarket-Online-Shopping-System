package com.soss;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.OrderStatus;
import com.soss.model.Role;
import com.soss.model.User;
import com.soss.repository.OrderRepository;
import com.soss.repository.UserRepository;
import com.soss.service.OrderService;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.regex.Pattern;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:admin-access;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "inventory.local-login.enabled=false"
})
@AutoConfigureMockMvc
class AdminAccessTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;

    @Test
    void administratorCanRenderAdministrativePages() throws Exception {
        String dashboard = mvc.perform(get("/admin/dashboard").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertCurrentLink(dashboard, "/admin/dashboard");

        String users = mvc.perform(get("/admin/users").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertCurrentLink(users, "/admin/users");
        assertThat(isCurrentLink(users, "/admin/dashboard")).isFalse();

        String ordersPage = mvc.perform(get("/admin/orders").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertCurrentLink(ordersPage, "/admin/orders");
        assertThat(ordersPage).contains("Manage Users &amp; Roles", "/inventory", "/delivery/manage", "/support/manage");

        String inventory = mvc.perform(get("/inventory").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertCurrentLink(inventory, "/inventory");

        String delivery = mvc.perform(get("/delivery/manage").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertCurrentLink(delivery, "/delivery/manage");

        String support = mvc.perform(get("/support/manage").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertCurrentLink(support, "/support/manage");
    }

    @Test
    void navigationOnlyShowsLinksAppropriateForTheCurrentRole() throws Exception {
        String admin = mvc.perform(get("/admin/dashboard").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(admin).contains("Admin Dashboard", "Manage Users &amp; Roles", "Customer Support", "Orders", "Profile", "Logout",
                "action=\"/logout\"", "aria-label=\"Admin management menu\"", "aria-controls=\"site-nav-links\"")
                .doesNotContain("href=\"/cart\"");

        String staff = mvc.perform(get("/staff/orders").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(staff).contains("Inventory", "Delivery", "Support", "Profile")
                .doesNotContain("Admin Dashboard", "Manage Users &amp; Roles", "href=\"/cart\"");

        User driver = users.findByEmail("nav-driver@example.com").orElseGet(() -> {
            User account = new User();
            account.setFullName("Navigation Driver");
            account.setEmail("nav-driver@example.com");
            account.setPasswordHash("unused");
            account.setPhone("9000000");
            account.setRole(Role.DELIVERY);
            return users.saveAndFlush(account);
        });
        String delivery = mvc.perform(get("/delivery/manage").with(user(driver.getEmail()).roles("DELIVERY")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(delivery).contains("Delivery", "Profile")
                .doesNotContain("Inventory", "Manage Users &amp; Roles", "href=\"/cart\"");

        String customer = mvc.perform(get("/products").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertCurrentLink(customer, "/products");
        assertThat(customer).contains("Cart", "Orders", "Profile")
                .doesNotContain("Manage Users &amp; Roles", "href=\"/inventory\"", "href=\"/delivery/manage\"");
    }

    @Test
    void customersStaffAndDeliveryCannotOpenAdminPages() throws Exception {
        for (String role : new String[]{"CUSTOMER", "STAFF", "DELIVERY"}) {
            mvc.perform(get("/admin/dashboard").with(user(role.toLowerCase()).roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/admin/users").with(user(role.toLowerCase()).roles(role)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/staff/orders").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/orders").with(user("delivery").roles("DELIVERY")))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffAreaRedirectsToOperationalInventoryPage() throws Exception {
        mvc.perform(get("/staff").with(user("staff").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/inventory"));
    }

    @Test
    void staffCanRenderQueueAndMovePaidOrderToProcessing() throws Exception {
        Order order = new Order();
        order.setCustomerId(42L);
        OrderItem item = new OrderItem();
        item.setProductId(8L);
        item.setProductName("Milk");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("4.00"));
        order.addItem(item);
        order = orderService.createOrder(order);
        order.setStatus(OrderStatus.PAID);
        order = orders.saveAndFlush(order);

        mvc.perform(get("/staff/orders").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("Milk × 2")));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/staff/orders/" + order.getId() + "/processing")
                        .with(user("staff").roles("STAFF"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/staff/orders"));
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    private void assertCurrentLink(String html, String href) {
        assertThat(isCurrentLink(html, href)).as("expected current navigation link %s", href).isTrue();
    }

    private boolean isCurrentLink(String html, String href) {
        String linkPattern = "<a(?=[^>]*href=\"" + Pattern.quote(href)
                + "\")(?=[^>]*class=\"[^\"]*active[^\"]*\")(?=[^>]*aria-current=\"page\")[^>]*>";
        return Pattern.compile(linkPattern, Pattern.DOTALL).matcher(html).find();
    }
}
