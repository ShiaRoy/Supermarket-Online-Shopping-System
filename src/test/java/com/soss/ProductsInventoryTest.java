package com.soss;

import com.soss.controller.product.ProductForm;
import com.soss.model.*;
import com.soss.repository.*;
import com.soss.service.CatalogueService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:products-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "inventory.local-login.enabled=false"
})
@AutoConfigureMockMvc
class ProductsInventoryTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogueService service;
    @Autowired InventoryRepository inventory;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    Long suva, nadi, milk;

    @BeforeEach
    void setup() {
        inventory.deleteAll(); products.deleteAll(); stores.deleteAll();
        suva = service.addStore("Suva").getId();
        nadi = service.addStore("Nadi").getId();
        milk = service.save(null, form("Fresh Milk 2L", "4.50", 8)).getId();
    }
    ProductForm form(String name, String price, int stock) {
        ProductForm form = new ProductForm();
        form.setName(name); form.setCategory("Dairy"); form.setDescription("Fresh milk.");
        form.setPrice(new BigDecimal(price)); form.setStoreId(suva); form.setQuantity(stock);
        form.setIcon("milk"); return form;
    }
    @Test void catalogueSearchAndDetailRenderRealData() throws Exception {
        mvc.perform(get("/products").param("storeId", suva.toString()))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Fresh Milk 2L")));
        mvc.perform(get("/search").param("q", "MILK").param("storeId", suva.toString()).param("min", "4").param("max", "5").param("availability", "low"))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Fresh Milk 2L")));
        mvc.perform(get("/products/" + milk).param("storeId", suva.toString()))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("8 available at Suva")));
        assertThat(service.search(suva, "milk", "Dairy", new BigDecimal("5"), null, "", false)).isEmpty();
        assertThat(service.search(suva, "bread", "", null, null, "", false)).isEmpty();
    }
    @Test void stockIsPerStoreAndCannotBeOversold() {
        assertThat(service.detail(milk, nadi, false).getQuantity()).isZero();
        service.deductStock(milk, suva, 6);
        assertThat(service.detail(milk, suva, false).getQuantity()).isEqualTo(2);
        assertThatThrownBy(() -> service.deductStock(milk, suva, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deductStock(milk, suva, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.detail(milk, suva, false).getQuantity()).isEqualTo(2);
        assertThat(service.detail(milk, nadi, false).getQuantity()).isZero();
    }
    @Test void staleEditCannotOverwriteStockAndHiddenProductsStayPrivate() {
        ProductForm stale = service.editForm(milk, suva);
        service.deductStock(milk, suva, 1);
        assertThatThrownBy(() -> service.save(milk, stale)).isInstanceOf(OptimisticLockingFailureException.class);
        ProductForm current = service.editForm(milk, suva);
        current.setActive(false); service.save(milk, current);
        assertThat(service.search(suva, "", "", null, null, "", false)).isEmpty();
        assertThat(service.search(suva, "", "", null, null, "", true)).hasSize(1);
        assertThatThrownBy(() -> service.deductStock(milk, suva, 1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void inventoryRequiresStaffAndCsrf() throws Exception {
        mvc.perform(get("/inventory")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/inventory").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/inventory/stores").with(user("staff").roles("STAFF")).param("name", "Lautoka"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/inventory").with(user("staff").roles("STAFF"))).andExpect(status().isOk());
        mvc.perform(get("/inventory/" + milk + "/edit").param("storeId", suva.toString()).with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
        mvc.perform(get("/inventory/login")).andExpect(status().isOk());
    }
    @Test void validPostPersistsAndNegativeStockIsRejected() throws Exception {
        mvc.perform(post("/inventory").with(user("staff").roles("STAFF")).with(csrf())
            .param("name", "Bread").param("category", "Bakery").param("description", "White bread")
            .param("price", "3.20").param("icon", "bread").param("active", "true")
            .param("storeId", suva.toString()).param("quantity", "10"))
            .andExpect(status().is3xxRedirection());
        assertThat(service.search(suva, "bread", "", null, null, "", false)).hasSize(1);
        mvc.perform(post("/inventory").with(user("staff").roles("STAFF")).with(csrf())
            .param("name", "Invalid").param("category", "Dairy").param("description", "Milk")
            .param("price", "1.00").param("icon", "milk").param("storeId", suva.toString()).param("quantity", "-1"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("productForm", "quantity"));
        assertThat(products.count()).isEqualTo(2);
    }
    @Test void newStoreGetsZeroStockForExistingProducts() {
        Long lautoka = service.addStore("Lautoka").getId();
        assertThat(service.detail(milk, lautoka, false).getQuantity()).isZero();
    }
    @Test void emptyDatabaseAndUnknownProductsHaveUsefulResponses() throws Exception {
        mvc.perform(get("/products/999999").param("storeId", suva.toString())).andExpect(status().isNotFound());
        inventory.deleteAll(); products.deleteAll(); stores.deleteAll();
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/inventory").with(user("staff").roles("STAFF"))).andExpect(status().isOk());
    }
}
