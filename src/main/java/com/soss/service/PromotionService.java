package com.soss.service;

import com.soss.model.*;
import com.soss.repository.*;
import com.soss.controller.product.PromotionForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.OptimisticLockingFailureException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PromotionService {
    private final PromotionRepository promotions;
    private final ProductRepository products;
    public PromotionService(PromotionRepository promotions, ProductRepository products) {
        this.promotions = promotions; this.products = products;
    }
    public LocalDate today() { return LocalDate.now(ZoneId.of("Pacific/Fiji")); }
    public List<Product> products() { return products.findAll(org.springframework.data.domain.Sort.by("name")); }
    public List<Promotion> list() { return promotions.findAllByOrderByStartsOnDescIdDesc(); }
    public Map<Long, BigDecimal> activePrices() {
        Map<Long, BigDecimal> prices = new HashMap<>();
        LocalDate day = today();
        for (Promotion promotion : list()) if (promotion.appliesOn(day))
            prices.merge(promotion.getProduct().getId(), promotion.discountedPrice(), BigDecimal::min);
        return prices;
    }
    public BigDecimal price(Product product) {
        LocalDate day = today();
        return promotions.findByProductIdAndEnabledTrue(product.getId()).stream()
            .filter(p -> p.appliesOn(day)).map(Promotion::discountedPrice)
            .min(BigDecimal::compareTo).orElse(product.getPrice());
    }
    public PromotionForm form(Long id) {
        Promotion promotion = find(id);
        PromotionForm form = new PromotionForm();
        form.setProductId(promotion.getProduct().getId()); form.setVersion(promotion.getVersion());
        form.setName(promotion.getName()); form.setPercentOff(promotion.getPercentOff());
        form.setStartsOn(promotion.getStartsOn()); form.setEndsOn(promotion.getEndsOn()); form.setEnabled(promotion.isEnabled());
        return form;
    }
    private Promotion find(Long id) {
        return promotions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional
    public Promotion save(Long id, PromotionForm form) {
        if (form.getEndsOn().isBefore(form.getStartsOn())) throw new IllegalArgumentException("End date must be on or after start date.");
        // Serialize promotion writes for this product so overlapping offers cannot race.
        Product product = products.lockForPromotion(form.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Promotion promotion = id == null ? new Promotion(product) : find(id);
        if (id != null && (!Objects.equals(promotion.getVersion(), form.getVersion())
            || !promotion.getProduct().getId().equals(product.getId())))
            throw new OptimisticLockingFailureException("Promotion changed. Reload before saving.");
        if (form.isEnabled()) {
            for (Promotion other : promotions.findByProductIdAndEnabledTrue(product.getId())) {
                if (!Objects.equals(id, other.getId()) && !form.getEndsOn().isBefore(other.getStartsOn())
                    && !form.getStartsOn().isAfter(other.getEndsOn()))
                    throw new IllegalArgumentException("This product already has an enabled promotion during those dates.");
            }
        }
        promotion.update(form.getName().strip(), form.getPercentOff(), form.getStartsOn(), form.getEndsOn(), form.isEnabled());
        return promotions.save(promotion);
    }
}
