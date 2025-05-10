package pl.edu.agh.kis.pz1.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTest {
    @Test
    @DisplayName("Correcly initializes remaining limit")
    void correctlyInitializes() {
        List<String> promotions = new ArrayList<>();
        Order order = new Order("210", new BigDecimal(20), promotions, false, new BigDecimal(0));
        order.initializeRemainingValue();
        assertEquals(order.getValue(), order.getRemainingValueToPay());
    }
}
