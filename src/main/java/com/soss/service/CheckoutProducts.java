package com.soss.checkout;

import java.math.BigDecimal;
import java.util.Optional;

/** Cart-owned integration contract. Implement using the shared Products module. */
public interface CheckoutProducts {
    record ProductSnapshot(Long id, String name, String icon, BigDecimal price, boolean active) {}
    Optional<ProductSnapshot> find(Long productId);
}
