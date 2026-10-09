package com.soss.service;

import com.soss.model.Inventory;
import com.soss.model.Product;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogueCheckoutProducts implements CheckoutProducts {

    private final CatalogueService catalogue;
    private final PromotionService promotions;

    public CatalogueCheckoutProducts(CatalogueService catalogue, PromotionService promotions) {
        this.catalogue = catalogue;
        this.promotions = promotions;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSnapshot> find(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        Long storeId = catalogue.store(null).getId();
        // Include inactive/out-of-stock rows so the cart can display or remove them.
        return catalogue.search(storeId, "", "", null, null, "", true).stream()
                .filter(stock -> productId.equals(stock.getProduct().getId()))
                .findFirst()
                .map(this::snapshot);
    }

    private ProductSnapshot snapshot(Inventory stock) {
        Product product = stock.getProduct();
        return new ProductSnapshot(product.getId(), product.getName(),
                product.getIcon(), promotions.price(product),
                product.isActive() && stock.getQuantity() > 0,
                stock.getQuantity());
    }
}
