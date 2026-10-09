package com.soss.model;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards against returning to a MySQL enum mapping that rejects later status values. */
class PaymentSchemaMappingTest {

    @Test
    void paymentAndOrderStatusesUseExpandableVarcharColumns() throws Exception {
        assertThat(statusColumn(Payment.class).columnDefinition()).isEqualTo("varchar(20)");
        assertThat(statusColumn(Order.class).columnDefinition()).isEqualTo("varchar(30)");
        assertThat(PaymentStatus.REFUNDED.name()).isEqualTo("REFUNDED");
        assertThat(OrderStatus.CANCELLED.name()).isEqualTo("CANCELLED");
    }

    private Column statusColumn(Class<?> entityType) throws Exception {
        Field field = entityType.getDeclaredField("status");
        return field.getAnnotation(Column.class);
    }
}
