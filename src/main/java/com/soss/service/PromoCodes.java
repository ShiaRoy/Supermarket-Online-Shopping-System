package com.soss.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** One server-side policy for cart previews and persisted checkout totals. */
public final class PromoCodes {
    private PromoCodes() {}

    public static String normalize(String code) {
        String value = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (value.length() > 32) throw new IllegalArgumentException("Promo code is too long.");
        return value.isEmpty() ? null : value;
    }

    public static BigDecimal discount(String code, BigDecimal subtotal, BigDecimal delivery) {
        String normalized = normalize(code);
        if (normalized == null) return new BigDecimal("0.00");
        BigDecimal discount = switch (normalized) {
            case "BULA10" -> subtotal.multiply(new BigDecimal("0.10"));
            case "SAVE5" -> subtotal.compareTo(new BigDecimal("50.00")) >= 0
                    ? new BigDecimal("5.00") : BigDecimal.ZERO;
            case "FREEDEL" -> subtotal.compareTo(new BigDecimal("30.00")) >= 0
                    ? delivery : BigDecimal.ZERO;
            default -> throw new IllegalArgumentException("Promo code is not valid.");
        };
        return discount.min(subtotal.add(delivery)).setScale(2, RoundingMode.HALF_UP);
    }

    public static void requireEligible(String code, BigDecimal subtotal) {
        String normalized = normalize(code);
        if (normalized != null && subtotal.signum() == 0)
            throw new IllegalArgumentException("Add groceries before applying a promo code.");
        if ("SAVE5".equals(normalized) && subtotal.compareTo(new BigDecimal("50.00")) < 0)
            throw new IllegalArgumentException("SAVE5 requires at least FJ$ 50.00 in groceries.");
        if ("FREEDEL".equals(normalized) && subtotal.compareTo(new BigDecimal("30.00")) < 0)
            throw new IllegalArgumentException("FREEDEL requires at least FJ$ 30.00 in groceries.");
    }
}
