package com.soss.checkout;

public enum PaymentProvider {
    MPAISA("M-PAiSA"), MYCASH("MyCash"), CARD("Visa / Mastercard");

    public final String displayName;
    PaymentProvider(String displayName) { this.displayName = displayName; }

    public static PaymentProvider forMethod(String method) {
        for (var provider : values()) {
            if (provider.displayName.equals(method)) return provider;
        }
        throw new IllegalArgumentException("Unsupported payment method.");
    }
}
