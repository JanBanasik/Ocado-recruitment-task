package pl.edu.agh.kis.pz1.utils;

import org.junit.jupiter.api.Test; // Keep Test annotation
// Removed DisplayName annotation

import pl.edu.agh.kis.pz1.domain.Order;
import pl.edu.agh.kis.pz1.domain.PaymentMethod;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class JsonParserTest {

    // Method for getting correct resource paths when running mvn test
    private String getResourcePath(String resourceName) {
        try {
            return Paths.get(ClassLoader.getSystemResource(resourceName).toURI()).toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("Error getting resource path: " + resourceName, e);
        }
    }

    @Test
    void testOrders2() throws IOException {
        String ordersPath = getResourcePath("orders2.json");
        String paymentmethodsPath = getResourcePath("paymentmethods2.json");
        List<Order> orders = JsonParser.parseOrders(ordersPath);

        assertNotNull(orders, "Orders list should not be null after parsing");
        assertEquals(3, orders.size(), "Should parse 3 orders from orders2.json");

        List<PaymentMethod> paymentMethods= JsonParser.parsePaymentMethods(paymentmethodsPath);
        assertEquals(2, paymentMethods.size());


        assertEquals(20, paymentMethods.getFirst().getDiscount());


        assertEquals("mZysk", paymentMethods.get(1).getId());
    }

    @Test
    void testOrders3() throws IOException {
        String ordersPath = getResourcePath("orders3.json");
        String paymentmethodsPath = getResourcePath("paymentmethods3.json");

        List<Order> orders = JsonParser.parseOrders(ordersPath);

        assertNotNull(orders, "Orders list should not be null after parsing");
        assertEquals(3, orders.size(), "Should parse 3 orders from orders2.json");

        List<PaymentMethod> paymentMethods= JsonParser.parsePaymentMethods(paymentmethodsPath);
        assertEquals(2, paymentMethods.size());


        assertEquals(10, paymentMethods.getFirst().getDiscount());

        assertEquals("BosBankrut", paymentMethods.get(1).getId());
    }

}