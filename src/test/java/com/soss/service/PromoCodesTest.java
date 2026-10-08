package com.soss.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PromoCodesTest {
    @Test
    void roundsOnceAndSupportsExactMinimumSpend() {
        assertThat(PromoCodes.discount("BULA10", new BigDecimal("4.55"), new BigDecimal("3.00")))
                .isEqualByComparingTo("0.46");
        PromoCodes.requireEligible("SAVE5", new BigDecimal("50.00"));
        PromoCodes.requireEligible("FREEDEL", new BigDecimal("30.00"));
        assertThatThrownBy(() -> PromoCodes.requireEligible("FREEDEL", new BigDecimal("29.99")))
                .hasMessageContaining("30.00");
        assertThatThrownBy(() -> PromoCodes.normalize("X".repeat(33)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromoCodes.requireEligible("BULA10", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
