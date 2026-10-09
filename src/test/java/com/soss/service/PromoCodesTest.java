package com.soss.service;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoCodesTest {
    private static BigDecimal money(String value) { return new BigDecimal(value); }

    @Test
    void roundsPercentageAndAcceptsInclusiveEligibilityThresholds() {
        assertThat(PromoCodes.discount("BULA10", money("4.55"), money("3.00")))
                .isEqualByComparingTo("0.46");
        PromoCodes.requireEligible("SAVE5", money("50.00"));
        PromoCodes.requireEligible("FREEDEL", money("30.00"));
        assertThatThrownBy(() -> PromoCodes.requireEligible("FREEDEL", money("29.99")))
                .hasMessageContaining("30.00");
        assertThatThrownBy(() -> PromoCodes.normalize("X".repeat(33)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromoCodes.requireEligible("BULA10", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void appliesPercentageThenFixedGroceryThenDeliveryDiscount() {
        String applied = PromoCodes.append(PromoCodes.append(PromoCodes.append(null, "FREEDEL"), "SAVE5"), "BULA10");
        List<PromoCodes.PromoDiscount> breakdown = PromoCodes.breakdown(applied, money("50.00"), money("3.00"));

        assertThat(breakdown).extracting(PromoCodes.PromoDiscount::code)
                .containsExactly("FREEDEL", "SAVE5", "BULA10");
        assertThat(breakdown).extracting(PromoCodes.PromoDiscount::amount)
                .containsExactly(money("3.00"), money("5.00"), money("5.00"));
        assertThat(PromoCodes.discount(applied, money("50.00"), money("3.00")))
                .isEqualByComparingTo("13.00");
        assertThat(money("50.00").add(money("3.00"))
                .subtract(PromoCodes.discount(applied, money("50.00"), money("3.00"))))
                .isEqualByComparingTo("40.00");
    }

    @Test
    void capsGroceryAndDeliveryDiscountsAndNeverReturnsNegativeTotals() {
        String applied = "BULA10,SAVE5,FREEDEL";
        assertThat(PromoCodes.discount(applied, money("30.00"), money("3.00")))
                .isEqualByComparingTo("6.00"); // BULA10 3.00 + FREEDEL 3.00; SAVE5 is ineligible.
        assertThat(PromoCodes.discount("BULA10", money("0.01"), money("0.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void appendsNormalizesRejectsDuplicatesAndRemovesOneCodeAtATime() {
        String applied = PromoCodes.append(PromoCodes.append(null, " bula10 "), "SAVE5");
        assertThat(applied).isEqualTo("BULA10,SAVE5");
        assertThatThrownBy(() -> PromoCodes.append(applied, "BULA10"))
                .hasMessageContaining("already applied");
        assertThat(PromoCodes.remove(applied, "save5")).isEqualTo("BULA10");
        assertThat(PromoCodes.remove("BULA10", "BULA10")).isNull();
        assertThatThrownBy(() -> PromoCodes.append(applied, "EXPIRED"))
                .hasMessageContaining("invalid or expired");
    }

    @Test
    void detectsEligibilityAfterCartQuantityChanges() {
        String applied = "BULA10,SAVE5,FREEDEL";
        var smallerBasket = PromoCodes.breakdown(applied, money("29.99"), money("3.00"));
        assertThat(smallerBasket).filteredOn(PromoCodes.PromoDiscount::eligible)
                .extracting(PromoCodes.PromoDiscount::code).containsExactly("BULA10");
        assertThatThrownBy(() -> PromoCodes.requireEligible(applied, money("29.99")))
                .hasMessageContaining("50.00");
    }
}