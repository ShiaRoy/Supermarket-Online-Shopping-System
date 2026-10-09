package com.soss;

import com.soss.controller.product.*;
import com.soss.model.*;
import com.soss.repository.*;
import com.soss.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:phase-two;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class ProductsPhaseTwoTest {
    @Autowired CatalogueService catalogue;
    @Autowired PromotionService promotions;
    @Autowired CheckoutProducts checkout;
    @Autowired InventoryRepository inventory;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired StockMovementRepository movements;
    @Autowired PromotionRepository offers;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    Long storeId, productId, otherStore;

    @BeforeEach void setup() {
        offers.deleteAll(); movements.deleteAll(); inventory.deleteAll(); products.deleteAll(); stores.deleteAll();
        storeId = catalogue.addStore("Suva").getId();
        otherStore = catalogue.addStore("Nadi").getId();
        ProductForm form = new ProductForm();
        form.setStoreId(storeId); form.setName("Phase Two Milk"); form.setCategory("Dairy");
        form.setDescription("Milk"); form.setPrice(new BigDecimal("10.00")); form.setIcon("milk");
        form.setQuantity(8); form.setLowStockThreshold(5);
        productId = catalogue.save(null, form).getId();
    }
    PromotionForm offer(LocalDate start, LocalDate end) {
        PromotionForm form = new PromotionForm(); form.setProductId(productId); form.setName("Milk special");
        form.setPercentOff(new BigDecimal("20")); form.setStartsOn(start); form.setEndsOn(end); return form;
    }
    ProductForm productForm(String name, String price, int quantity) {
        ProductForm form = new ProductForm();
        form.setStoreId(storeId); form.setName(name); form.setCategory("Dairy");
        form.setDescription(name); form.setPrice(new BigDecimal(price)); form.setIcon("basket");
        form.setQuantity(quantity); form.setLowStockThreshold(2);
        return form;
    }
    @Test void thresholdsAndAlertsAreStoreSpecificAndIncludeZero() throws Exception {
        assertThat(catalogue.detail(productId, storeId, true).isLowStock()).isFalse();
        ProductForm form = catalogue.editForm(productId, storeId); form.setLowStockThreshold(8);
        catalogue.save(productId, form);
        assertThat(catalogue.search(storeId, "", "", null, null, "alerts", true)).hasSize(1);
        assertThat(catalogue.detail(productId, otherStore, true).getLowStockThreshold()).isEqualTo(10);
        mvc.perform(get("/inventory").param("storeId", storeId.toString()).with(user("staff").roles("STAFF")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("1 products need restocking at Suva.")));
        catalogue.deductStock(productId, storeId, 8);
        assertThat(catalogue.search(storeId, "", "", null, null, "alerts", true)).hasSize(1);
        assertThat(catalogue.detail(productId, storeId, true).getStatus()).isEqualTo("Out of stock");
    }
    @Test @WithMockUser(username = "divit", roles = "STAFF")
    void restockAndSaleRecordActorReasonAndBalances() {
        catalogue.restock(productId, storeId, 12, "Delivery INV-42");
        catalogue.deductStock(productId, storeId, 3);
        var history = catalogue.history(productId, storeId, 0).getContent();
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getType()).isEqualTo("SALE");
        assertThat(history.get(0).getBeforeQuantity()).isEqualTo(20);
        assertThat(history.get(0).getAfterQuantity()).isEqualTo(17);
        assertThat(history.get(1).getReason()).isEqualTo("Delivery INV-42");
        assertThat(history.get(1).getChange()).isEqualTo(12);
        assertThat(history.get(1).getActor()).isEqualTo("divit");
        assertThat(catalogue.detail(productId, otherStore, true).getQuantity()).isZero();
    }
    @Test void stockAndHistoryRollbackTogether() {
        long before = movements.count();
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> transaction.execute(status -> {
            catalogue.deductStock(productId, storeId, 2);
            throw new IllegalStateException("Payment transaction failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(8);
        assertThat(movements.count()).isEqualTo(before);
        assertThatThrownBy(() -> catalogue.restock(productId, storeId, 0, "Invalid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalogue.restock(productId, storeId, 1000000, "Overflow")).isInstanceOf(IllegalArgumentException.class);
        assertThat(movements.count()).isEqualTo(before);
    }
    @Test void quantityEditsAreAuditedButMetadataOnlyEditsAreNot() {
        ProductForm form = catalogue.editForm(productId, storeId);
        form.setQuantity(6); form.setStockReason("Damaged cartons"); catalogue.save(productId, form);
        var movement = catalogue.history(productId, storeId, 0).getContent().getFirst();
        assertThat(movement.getType()).isEqualTo("ADJUSTMENT");
        assertThat(movement.getReason()).isEqualTo("Damaged cartons");
        long before = movements.count();
        form = catalogue.editForm(productId, storeId); form.setLowStockThreshold(7); catalogue.save(productId, form);
        assertThat(movements.count()).isEqualTo(before);
    }
    @Test void activePromotionUsesSamePriceInCatalogueSearchAndCheckout() throws Exception {
        LocalDate today = promotions.today();
        promotions.save(null, offer(today, today));
        assertThat(checkout.find(productId).orElseThrow().price()).isEqualByComparingTo("8.00");
        mvc.perform(get("/search").param("storeId", storeId.toString()).param("min", "7.90").param("max", "8.10"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("FJ$ 8.00")))
            .andExpect(content().string(containsString("Phase Two Milk")));
        mvc.perform(get("/products/" + productId).param("storeId", storeId.toString()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("FJ$ 8.00")));
    }
    @Test void catalogueCombinesCategorySearchAndPriceSorting() throws Exception {
        catalogue.save(null, productForm("Cheese", "5.00", 5));
        ProductForm produce = productForm("Apple", "2.00", 5);
        produce.setCategory("Produce");
        catalogue.save(null, produce);

        String ascending = mvc.perform(get("/products").param("category", "Dairy")
                        .param("q", "").param("sort", "price-asc"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Cheese")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Apple"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(ascending.indexOf("Cheese")).isLessThan(ascending.indexOf("Phase Two Milk"));

        String descending = mvc.perform(get("/products").param("category", "Dairy")
                        .param("q", "Milk").param("sort", "price-desc"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Phase Two Milk")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Cheese"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(descending).contains("Phase Two Milk");

        String grouped = mvc.perform(get("/products").param("storeId", storeId.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(grouped).contains("Shop by category", "Dairy", "Produce", "All products");
    }

    @Test void catalogueCategorySearchPriceAvailabilityAndSortFiltersWorkTogether() throws Exception {
        ProductForm juice = productForm("Juice", "8.00", 5);
        juice.setCategory("Beverages");
        catalogue.save(null, juice);
        ProductForm tea = productForm("Tea", "5.00", 5);
        tea.setCategory("Beverages");
        catalogue.save(null, tea);
        ProductForm soda = productForm("Soda", "11.00", 5);
        soda.setCategory("Beverages");
        catalogue.save(null, soda);

        String result = mvc.perform(get("/products")
                        .param("storeId", storeId.toString())
                        .param("q", "")
                        .param("category", "Beverages")
                        .param("min", "7")
                        .param("max", "10")
                        .param("availability", "in")
                        .param("sort", "price-asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Juice")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Tea"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Soda"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(result).contains("Beverages", "1 item", "min=7", "max=10", "availability=in", "sort=price-asc");
    }
    @Test void promotionWindowsDisabledStateAndHiddenProductAreRespected() {
        LocalDate today = promotions.today();
        Promotion promotion = promotions.save(null, offer(today.plusDays(1), today.plusDays(2)));
        assertThat(checkout.find(productId).orElseThrow().price()).isEqualByComparingTo("10.00");
        PromotionForm edit = promotions.form(promotion.getId()); edit.setStartsOn(today.minusDays(2)); edit.setEndsOn(today.minusDays(1));
        promotions.save(promotion.getId(), edit);
        assertThat(promotions.activePrices()).isEmpty();
        edit = promotions.form(promotion.getId()); edit.setStartsOn(today); edit.setEndsOn(today); edit.setEnabled(false);
        promotions.save(promotion.getId(), edit); assertThat(promotions.activePrices()).isEmpty();
        edit = promotions.form(promotion.getId()); edit.setEnabled(true); promotions.save(promotion.getId(), edit);
        ProductForm product = catalogue.editForm(productId, storeId); product.setActive(false); catalogue.save(productId, product);
        assertThat(promotions.activePrices()).isEmpty();
    }
    @Test void overlappingAndStalePromotionsAreRejected() {
        LocalDate today = promotions.today();
        Promotion promotion = promotions.save(null, offer(today, today.plusDays(2)));
        assertThatThrownBy(() -> promotions.save(null, offer(today.plusDays(2), today.plusDays(3))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> promotions.save(null, offer(today.plusDays(5), today)))
            .isInstanceOf(IllegalArgumentException.class);
        PromotionForm stale = promotions.form(promotion.getId());
        PromotionForm edit = promotions.form(promotion.getId()); edit.setName("Updated"); promotions.save(promotion.getId(), edit);
        assertThatThrownBy(() -> promotions.save(promotion.getId(), stale)).isInstanceOf(OptimisticLockingFailureException.class);
    }
    @Test void newStaffPagesRenderAndRejectCustomerWritesAndMissingCsrf() throws Exception {
        for (String path : new String[]{"/inventory/promotions", "/inventory/promotions/new", "/inventory/" + productId + "/history?storeId=" + storeId}) {
            mvc.perform(get(path).with(user("staff").roles("STAFF"))).andExpect(status().isOk());
            mvc.perform(get(path).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/inventory/" + productId + "/restock").with(user("staff").roles("STAFF"))
            .param("storeId", storeId.toString()).param("amount", "2").param("reason", "Received"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/inventory/" + productId + "/restock").with(user("staff").roles("STAFF")).with(csrf())
            .param("storeId", storeId.toString()).param("amount", "2").param("reason", "Received"))
            .andExpect(status().is3xxRedirection());
        assertThat(catalogue.detail(productId, storeId, true).getQuantity()).isEqualTo(10);
    }
    @Test void promotionPostValidatesAndPersists() throws Exception {
        mvc.perform(post("/inventory/promotions").with(user("staff").roles("STAFF")).with(csrf())
            .param("name", "Sale").param("productId", productId.toString()).param("percentOff", "150")
            .param("startsOn", promotions.today().toString()).param("endsOn", promotions.today().toString()))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("promotionForm", "percentOff"));
        assertThat(offers.count()).isZero();
        mvc.perform(post("/inventory/promotions").with(user("admin").roles("ADMIN")).with(csrf())
            .param("name", "Sale").param("productId", productId.toString()).param("percentOff", "25").param("enabled", "true")
            .param("startsOn", promotions.today().toString()).param("endsOn", promotions.today().toString()))
            .andExpect(status().is3xxRedirection());
        Long id = offers.findAll().getFirst().getId();
        mvc.perform(get("/inventory/promotions/" + id + "/edit").with(user("staff").roles("STAFF")))
            .andExpect(status().isOk());
        assertThat(checkout.find(productId).orElseThrow().price()).isEqualByComparingTo("7.50");
    }
}
