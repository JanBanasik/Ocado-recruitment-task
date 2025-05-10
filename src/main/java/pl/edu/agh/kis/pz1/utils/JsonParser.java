package pl.edu.agh.kis.pz1.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.edu.agh.kis.pz1.domain.Order;
import pl.edu.agh.kis.pz1.domain.PaymentMethod;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for parsing JSON files containing Order and PaymentMethod data.
 * Uses the Jackson library for JSON processing.
 */
public class JsonParser {

    // ObjectMapper instance for performing JSON reading and writing
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private JsonParser() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parses a JSON file containing a list of orders into a List of Order objects.
     * Initializes the remaining value to pay for each order after parsing.
     *
     * @param path The absolute path to the orders JSON file.
     * @return A List of populated Order objects.
     * @throws IOException If an error occurs while reading or parsing the file.
     */
    public static List<Order> parseOrders(String path) throws IOException {
        File file = new File(path);

        // Read the JSON file and map its content to a List of Order objects
        // Using TypeReference with empty diamond <> works with newer Java versions
        List<Order> orders = mapper.readValue(file, new TypeReference<>() {});

        // Initialize the remaining value to pay for each order after parsing
        orders.forEach(Order::initializeRemainingValue);
        return orders;
    }

    /**
     * Parses a JSON file containing a list of payment methods into a List of PaymentMethod objects.
     * Initializes the remaining limit for each payment method after parsing.
     *
     * @param path The absolute path to the payment methods JSON file.
     * @return A List of populated PaymentMethod objects.
     * @throws IOException If an error occurs while reading or parsing the file.
     */
    public static List<PaymentMethod> parsePaymentMethods(String path) throws IOException {
        File file = new File(path);
        // Read the JSON file and map its content to a List of PaymentMethod objects
        List<PaymentMethod> paymentMethods = mapper.readValue(file, new TypeReference<>() {});

        // Initialize the remaining limit for each payment method after parsing
        paymentMethods.forEach(PaymentMethod::initializeRemainingLimit);
        return paymentMethods;
    }
}