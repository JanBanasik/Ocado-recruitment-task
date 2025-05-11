// pl.edu.agh.kis.pz1.Main.java
package pl.edu.agh.kis.pz1;

import pl.edu.agh.kis.pz1.domain.Order;
import pl.edu.agh.kis.pz1.domain.PaymentMethod;
import pl.edu.agh.kis.pz1.domain.Result;
import pl.edu.agh.kis.pz1.optimizer.NotFoundPaymentsException;
import pl.edu.agh.kis.pz1.optimizer.PaymentOptimizer;
import pl.edu.agh.kis.pz1.utils.JsonParser;

import java.io.IOException;
import java.util.List;


/**
 * Main entry point for the Payment Optimization application.
 * This class handles command-line arguments, parses input JSON files,
 * runs the payment optimization logic, and prints the results.
 */
public class Main {
    /**
     * The main method that starts the payment optimization process.
     * It expects two command-line arguments: the paths to the orders JSON file
     * and the payment methods JSON file.
     *
     * @param args Command-line arguments: args[0] = path to orders JSON, args[1] = path to payment methods JSON.
     */
    public static void main(String... args) {

        if (args.length < 2) {
            System.err.println("Usage: java -jar target\\app.jar <orders_file_path> <payment_methods_file_path>");
            System.exit(1);
        }

        String ordersPath = args[0];
        String paymentMethodsPath = args[1];

        List<Order> orders;
        List<PaymentMethod> paymentMethods;

        // Loading data for orders
        try {
            orders = JsonParser.parseOrders(ordersPath);
        } catch (IOException e) {
            System.err.println("Error reading or parsing orders file: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Loading data for payment methods
        try {
            paymentMethods = JsonParser.parsePaymentMethods(paymentMethodsPath);
        } catch (IOException e) {
            System.err.println("Error reading or parsing payment methods file: " + e.getMessage());
            System.exit(1);
            return;
        }

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, paymentMethods);
        List<Result> results;
        try {
            results = optimizer.optimize();
        } catch (RuntimeException | NotFoundPaymentsException e) {
            System.err.println("Optimization failed: " + e.getMessage());
            System.exit(1);
            return;
        }

        for (Result result : results) {
            System.out.println(result);
        }

    }
}