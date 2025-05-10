package pl.edu.agh.kis.pz1.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentMethodTest {

    @Test
    @DisplayName("Correctly initializes remaining limit and performs addition and deduction")
    void correctlyInitializesRemainingLimit() {
        PaymentMethod paymentMethod = new PaymentMethod("200", 20, new BigDecimal(20), null, new BigDecimal(0));
        paymentMethod.initializeRemainingLimit();

        assertEquals(paymentMethod.getLimit(), paymentMethod.getRemainingLimit());

        paymentMethod.deductLimit(new BigDecimal(20));
        assertEquals(BigDecimal.ZERO, paymentMethod.getRemainingLimit());

        paymentMethod.addSpent(new BigDecimal(500));
        assertEquals(paymentMethod.getTotalSpent(), new BigDecimal(500));

    }
}
