package com.soss.service;

import com.soss.model.*;
import com.soss.repository.*;
import com.soss.controller.product.ProductForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CatalogueService {
    private final ProductRepository products;
    private final StoreRepository stores;
    private final InventoryRepository inventory;
    public CatalogueService(ProductRepository products, StoreRepository stores, InventoryRepository inventory) {
        this.products = products; this.stores = stores; this.inventory = inventory;
    }
    public List<Store> stores() { return stores.findAll(); }
    @Transactional
    public Store addStore(String name) {
        Store store = stores.save(new Store(name));
        for (Product product : products.findAll()) inventory.save(new Inventory(store, product, 0));
        return store;
    }
    public Store store(Long id) {
        if (id == null) return stores.findAll().stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No stores configured"));
        return stores.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public List<Inventory> search(Long storeId, String q, String category, BigDecimal min, BigDecimal max, String availability, boolean staff) {
        String term = q == null ? "" : q.strip().toLowerCase(Locale.ROOT);
        return inventory.findByStoreIdOrderByProductNameAsc(storeId).stream()
            .filter(i -> staff || i.getProduct().isActive())
            .filter(i -> i.getProduct().getName().toLowerCase(Locale.ROOT).contains(term)
                || i.getProduct().getDescription().toLowerCase(Locale.ROOT).contains(term))
            .filter(i -> category == null || category.isBlank() || i.getProduct().getCategory().equals(category))
            .filter(i -> min == null || i.getProduct().getPrice().compareTo(min) >= 0)
            .filter(i -> max == null || i.getProduct().getPrice().compareTo(max) <= 0)
            .filter(i -> switch (availability == null ? "" : availability) {
                case "in" -> i.getQuantity() > 0;
                case "low" -> i.getQuantity() > 0 && i.getQuantity() <= 10;
                case "out" -> i.getQuantity() == 0;
                default -> true;
            }).toList();
    }
    public List<String> categories(Long storeId, boolean staff) {
        return search(storeId, "", "", null, null, "", staff).stream()
            .map(i -> i.getProduct().getCategory()).distinct().sorted().toList();
    }
    public Inventory detail(Long productId, Long storeId, boolean staff) {
        Inventory item = inventory.findByProductIdAndStoreId(productId, storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!staff && !item.getProduct().isActive()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return item;
    }
    public ProductForm editForm(Long productId, Long storeId) {
        Inventory item = detail(productId, storeId, true);
        Product p = item.getProduct();
        ProductForm form = new ProductForm();
        form.setName(p.getName()); form.setCategory(p.getCategory()); form.setDescription(p.getDescription());
        form.setPrice(p.getPrice()); form.setIcon(p.getIcon()); form.setActive(p.isActive());
        form.setStoreId(storeId); form.setQuantity(item.getQuantity());
        form.setProductVersion(p.getVersion()); form.setInventoryVersion(item.getVersion());
        return form;
    }
    @Transactional
    public Product save(Long id, ProductForm form) {
        Store selectedStore = store(form.getStoreId());
        Product product;
        if (id == null) {
            product = products.save(new Product(form.getName().strip(), form.getCategory().strip(),
                form.getDescription().strip(), form.getPrice(), form.getIcon()));
            product.update(product.getName(), product.getCategory(), product.getDescription(),
                product.getPrice(), product.getIcon(), form.isActive());
            // Every branch gets a stock row; only the selected branch gets the entered quantity.
            for (Store branch : stores.findAll()) {
                inventory.save(new Inventory(branch, product, branch.getId().equals(selectedStore.getId()) ? form.getQuantity() : 0));
            }
        } else {
            Inventory item = detail(id, selectedStore.getId(), true);
            product = item.getProduct();
            if (!Objects.equals(product.getVersion(), form.getProductVersion())
                || !Objects.equals(item.getVersion(), form.getInventoryVersion())) {
                throw new OptimisticLockingFailureException("Another update was saved. Reload the product before editing again.");
            }
            product.update(form.getName().strip(), form.getCategory().strip(), form.getDescription().strip(),
                form.getPrice(), form.getIcon(), form.isActive());
            item.setQuantity(form.getQuantity());
        }
        return product;
    }
    // The order/payment module must call this once inside its successful-payment transaction.
    @Transactional
    public void deductStock(Long productId, Long storeId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        Inventory item = inventory.lockStock(productId, storeId)
            .orElseThrow(() -> new IllegalArgumentException("Product is not stocked at this store."));
        if (!item.getProduct().isActive() || item.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient available stock.");
        }
        item.setQuantity(item.getQuantity() - quantity);
    }
}
