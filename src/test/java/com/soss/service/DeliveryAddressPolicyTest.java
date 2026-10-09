package com.soss.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryAddressPolicyTest {
    @Test
    void composesStructuredAddressAndOptionalInstructions() {
        assertThat(DeliveryAddressPolicy.compose("Suva", "Victoria Parade", "12", "Blue gate"))
                .isEqualTo("12 Victoria Parade, Suva, Instructions: Blue gate");
    }

    @Test
    void rejectsUnsupportedAreaIncompleteFieldsAndPlaceholderText() {
        assertThatThrownBy(() -> DeliveryAddressPolicy.compose("Lami", "Victoria Parade", "12", null))
                .hasMessageContaining("supported delivery area");
        assertThatThrownBy(() -> DeliveryAddressPolicy.compose("Suva", "sdfghj", "12", null))
                .hasMessageContaining("street or road");
        assertThatThrownBy(() -> DeliveryAddressPolicy.compose("Suva", "Victoria Parade", "Lot", null))
                .hasMessageContaining("building number");
        assertThatThrownBy(() -> DeliveryAddressPolicy.validate("sdfghj"))
                .hasMessageContaining("complete delivery address");
    }
}