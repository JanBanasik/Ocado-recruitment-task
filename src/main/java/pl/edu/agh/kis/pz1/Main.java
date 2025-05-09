// pl.edu.agh.kis.pz1.Main.java
package pl.edu.agh.kis.pz1;

import pl.edu.agh.kis.pz1.domain.Order;
import pl.edu.agh.kis.pz1.domain.PaymentMethod;
import pl.edu.agh.kis.pz1.domain.Result;
import pl.edu.agh.kis.pz1.optimizer.PaymentOptimizer;
import pl.edu.agh.kis.pz1.utils.JsonParser;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String... args) {
        // Sprawdzenie liczby argumentów
        if (args.length < 2) {
            System.err.println("Usage: java -jar your_app.jar <orders_file_path> <payment_methods_file_path>");
            System.exit(1); // Zakończ program z kodem błędu
        }

        String ordersPath = args[0];
        String paymentMethodsPath = args[1];

        List<Order> orders;
        List<PaymentMethod> paymentMethods;

        // Wczytanie danych zamówień
        try {
            orders = JsonParser.parseOrders(ordersPath);
        } catch (IOException e) {
            System.err.println("Error reading or parsing orders file: " + e.getMessage());
            e.printStackTrace(); // Wypisz stos wywołań dla dokładniejszej diagnozy
            System.exit(1);
            return; // Dla pewności, choć exit już kończy
        }

        // Wczytanie danych metod płatności
        try {
            paymentMethods = JsonParser.parsePaymentMethods(paymentMethodsPath);
        } catch (IOException e) {
            System.err.println("Error reading or parsing payment methods file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
        }

        // Sprawdzenie, czy dane zostały wczytane (listy nie są null, choć puste mogą być dopuszczalne)
        if (orders == null || paymentMethods == null) {
            System.err.println("Failed to load data.");
            System.exit(1);
            return;
        }

        // Uruchomienie optymalizatora
        PaymentOptimizer optimizer = new PaymentOptimizer(orders, paymentMethods);
        List<Result> results;
        try {
            results = optimizer.optimize();
        } catch (RuntimeException e) {
            System.err.println("Optimization failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
        }


        // Wypisanie wyników
        for (Result result : results) {
            System.out.println(result);
        }

        // Opcjonalnie: Zakończenie programu z kodem sukcesu
        System.exit(0);
    }
}