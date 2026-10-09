package com.soss.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Practical checkout address checks; these checks improve completeness but cannot verify a real location. */
public final class DeliveryAddressPolicy {
    /** Only areas evidenced by current checkout/delivery flows; no wider coverage list is configured yet. */
    public static final List<String> AREAS = List.of("Suva", "Nausori");
    private static final Set<String> NONSENSE = Set.of("test", "asdf", "sdfghj", "qwerty", "unknown", "none", "n/a");
    private DeliveryAddressPolicy() {}

    public static String compose(String area, String street, String building, String instructions) {
        String selectedArea = clean(area);
        String road = clean(street);
        String number = clean(building);
        String notes = clean(instructions);
        if (selectedArea == null || !AREAS.contains(selectedArea)) {
            throw new IllegalArgumentException("Choose a supported delivery area.");
        }
        if (road == null || road.length() < 4 || !road.matches(".*[\\p{L}]{3,}.*") || isNonsense(road)) {
            throw new IllegalArgumentException("Enter a valid street or road name.");
        }
        if (number == null || !number.matches("(?=.*\\d)[\\p{L}\\p{N}/-]+(?:\\s[\\p{L}\\p{N}/-]+)*")) {
            throw new IllegalArgumentException("Enter a house, lot or building number containing at least one digit.");
        }
        if (notes != null && notes.length() > 160) {
            throw new IllegalArgumentException("Delivery instructions must be 160 characters or fewer.");
        }
        String address = number + " " + road + ", " + selectedArea;
        if (notes != null) address += ", Instructions: " + notes;
        return validate(address);
    }

    /** Validates saved-address and API/free-form compatibility paths as well as composed addresses. */
    public static String validate(String value) {
        String address = clean(value);
        if (address == null || address.length() < 8 || address.length() > 255
                || !address.matches(".*[\\p{L}]{3,}.*") || !address.matches(".*\\d.*")
                || isNonsense(address)) {
            throw new IllegalArgumentException("Enter a complete delivery address with a street name and building number.");
        }
        return address;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String result = value.strip().replaceAll("\\s+", " ");
        return result.isEmpty() ? null : result;
    }

    private static boolean isNonsense(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}/-]+", " ").strip();
        return NONSENSE.contains(normalized) || normalized.matches("(.)\\1{5,}");
    }
}