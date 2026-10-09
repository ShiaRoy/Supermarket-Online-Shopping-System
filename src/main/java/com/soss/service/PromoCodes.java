package com.soss.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic policy for cart previews and persisted checkout totals. */
public final class PromoCodes {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal FIVE = new BigDecimal("5.00");
    private static final BigDecimal MIN_SAVE5 = new BigDecimal("50.00");
    private static final BigDecimal MIN_FREE_DELIVERY = new BigDecimal("30.00");
    private PromoCodes() {}

    public record PromoDiscount(String code, String description, BigDecimal amount, boolean eligible) {}

    /** Normalize a single code entered by a customer. Stored code lists use commas as separators. */
    public static String normalize(String code) {
        String value = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (value.length() > 32 || value.contains(",")) {
            throw new IllegalArgumentException("Enter one promo code at a time (maximum 32 characters).");
        }
        return value.isEmpty() ? null : value;
    }

    public static List<String> codes(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String part : stored.split(",")) {
            String normalized = normalize(part);
            if (normalized == null) continue;
            requireKnown(normalized);
            if (result.contains(normalized)) {
                throw new IllegalArgumentException("The promo code " + normalized + " is already applied.");
            }
            result.add(normalized);
        }
        return List.copyOf(result);
    }

    public static String append(String stored, String code) {
        String normalized = normalize(code);
        if (normalized == null) throw new IllegalArgumentException("Enter a promo code.");
        requireKnown(normalized);
        List<String> applied = new ArrayList<>(codes(stored));
        if (applied.contains(normalized)) {
            throw new IllegalArgumentException(normalized + " is already applied to this cart.");
        }
        applied.add(normalized);
        String combined = String.join(",", applied);
        if (combined.length() > 32) throw new IllegalArgumentException("Too many promo codes are applied.");
        return combined;
    }

    public static String remove(String stored, String code) {
        String normalized = normalize(code);
        if (normalized == null) throw new IllegalArgumentException("Choose a promo code to remove.");
        List<String> applied = new ArrayList<>(codes(stored));
        if (!applied.remove(normalized)) {
            throw new IllegalArgumentException(normalized + " is not applied to this cart.");
        }
        return applied.isEmpty() ? null : String.join(",", applied);
    }

    /**
     * Discounts apply to the pre-promo grocery subtotal for eligibility. BULA10 is calculated
     * first, SAVE5 uses remaining grocery value second, and FREEDEL applies only to delivery.
     */
    public static List<PromoDiscount> breakdown(String stored, BigDecimal subtotal, BigDecimal delivery) {
        requireAmounts(subtotal, delivery);
        List<String> applied = codes(stored);
        BigDecimal groceryRemaining = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentage = applied.contains("BULA10") && subtotal.signum() > 0
                ? subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP).min(groceryRemaining)
                : ZERO;
        groceryRemaining = groceryRemaining.subtract(percentage).max(ZERO);
        BigDecimal fixed = applied.contains("SAVE5") && subtotal.compareTo(MIN_SAVE5) >= 0
                ? FIVE.min(groceryRemaining) : ZERO;
        boolean deliveryEligible = applied.contains("FREEDEL") && subtotal.compareTo(MIN_FREE_DELIVERY) >= 0;
        BigDecimal deliveryDiscount = deliveryEligible ? delivery.setScale(2, RoundingMode.HALF_UP) : ZERO;

        List<PromoDiscount> result = new ArrayList<>();
        for (String code : applied) {
            switch (code) {
                case "BULA10" -> result.add(new PromoDiscount(code, "10% off groceries",
                        percentage, subtotal.signum() > 0));
                case "SAVE5" -> result.add(new PromoDiscount(code,
                        "FJ$ 5 off grocery orders of FJ$ 50 or more (inclusive)", fixed,
                        subtotal.compareTo(MIN_SAVE5) >= 0));
                case "FREEDEL" -> result.add(new PromoDiscount(code,
                        "Free delivery on orders of FJ$ 30 or more (inclusive)", deliveryDiscount,
                        deliveryEligible));
                default -> throw new IllegalArgumentException("Promo code is not valid.");
            }
        }
        return List.copyOf(result);
    }

    public static BigDecimal discount(String stored, BigDecimal subtotal, BigDecimal delivery) {
        return breakdown(stored, subtotal, delivery).stream()
                .map(PromoDiscount::amount).reduce(ZERO, BigDecimal::add)
                .min(subtotal.add(delivery)).max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public static void requireEligible(String stored, BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Grocery subtotal is invalid.");
        }
        for (String code : codes(stored)) {
            switch (code) {
                case "BULA10" -> {
                    if (subtotal.signum() == 0) throw new IllegalArgumentException("Add groceries before applying a promo code.");
                }
                case "SAVE5" -> {
                    if (subtotal.compareTo(MIN_SAVE5) < 0) throw new IllegalArgumentException("SAVE5 requires at least FJ$ 50.00 in groceries.");
                }
                case "FREEDEL" -> {
                    if (subtotal.compareTo(MIN_FREE_DELIVERY) < 0) throw new IllegalArgumentException("FREEDEL requires at least FJ$ 30.00 in groceries.");
                }
                default -> throw new IllegalArgumentException("Promo code is not valid.");
            }
        }
    }

    private static void requireKnown(String code) {
        if (!List.of("BULA10", "SAVE5", "FREEDEL").contains(code)) {
            throw new IllegalArgumentException("Promo code " + code + " is invalid or expired.");
        }
    }

    private static void requireAmounts(BigDecimal subtotal, BigDecimal delivery) {
        if (subtotal == null || delivery == null || subtotal.signum() < 0 || delivery.signum() < 0) {
            throw new IllegalArgumentException("Cart amounts are invalid.");
        }
    }
}