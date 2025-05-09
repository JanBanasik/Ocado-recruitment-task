package pl.edu.agh.kis.pz1.optimizer;

import pl.edu.agh.kis.pz1.domain.Order;
import pl.edu.agh.kis.pz1.domain.PaymentMethod;
import pl.edu.agh.kis.pz1.domain.Result;
import pl.edu.agh.kis.pz1.utils.BigDecimalUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

// Klasa odpowiedzialna za alokację płatności i obliczenie sumarycznych wydatków
public class PaymentOptimizer {

    private final List<Order> orders;
    private final Map<String, PaymentMethod> paymentMethodsMap;
    private final PaymentMethod pointsMethod; // Wygodne referencja do metody PUNKTY

    private static final String POINTS_METHOD_ID = "PUNKTY";
    private static final BigDecimal MIN_POINTS_PERCENTAGE_FOR_R3 = BigDecimal.valueOf(10); // 10%

    public PaymentOptimizer(List<Order> orders, List<PaymentMethod> paymentMethods) {
        this.orders = orders;
        // Konwertujemy listę metod płatności na mapę dla łatwego dostępu po ID
        this.paymentMethodsMap = paymentMethods.stream()
                .collect(Collectors.toMap(PaymentMethod::getId, pm -> pm));

        // Pobieramy referencję do metody PUNKTY
        this.pointsMethod = paymentMethodsMap.get(POINTS_METHOD_ID);

        // Sprawdzenie czy metoda PUNKTY istnieje (wymagane dla R3 i R4)
        if (pointsMethod == null) {
            System.err.println("Warning: Payment method 'PUNKTY' not found. R3 and R4 promotions will not be available.");
            // Nie rzucamy błędu, ale te opcje płatności nie będą brane pod uwagę
        }

        // Dodatkowe sprawdzenie: czy istnieje jakakolwiek inna metoda płatności (karta)?
        boolean anyCardMethodExists = paymentMethods.stream()
                .anyMatch(pm -> !pm.getId().equals(POINTS_METHOD_ID));
        if (!anyCardMethodExists) {
            System.err.println("Warning: No card payment methods found. Only PUNKTY payments are possible if available.");
        }
    }

    // Główna metoda wykonująca optymalizację
    public List<Result> optimize() {
        // Krok 1: Zainicjowanie stanów w obiektach Order i PaymentMethod
        orders.forEach(Order::initializeRemainingValue); // Robione już w JsonParser, ale można powtórzyć dla pewności
        paymentMethodsMap.values().forEach(PaymentMethod::initializeRemainingLimit); // Robione już w JsonParser

        // Krok 2: Alokuj pełne płatności z największym rabatem (R2 i R4)
        allocateFullPaymentsWithDiscount();

        // Krok 3: Obsłuż pozostałe zamówienia (priorytet R3, potem 0%)
        allocateRemainingPayments();

        // Krok 4: Sprawdź, czy wszystkie zamówienia zostały opłacone
        verifyAllOrdersPaid();

        // Krok 5: Zbierz sumaryczne wydatki per metoda płatności
        return collectResults();
    }

    // --- Krok 2: Alokacja pełnych płatności z rabatem (R2 i R4) ---
    private void allocateFullPaymentsWithDiscount() {
        // Lista potencjalnych pełnych płatności, które możemy zastosować
        List<PotentialFullPayment> potentialPayments = new ArrayList<>();

        for (Order order : orders) {
            // Jeśli zamówienie już opłacone, pomiń
            if (order.isPaid()) {
                continue;
            }

            // Opcja R4: Pełna płatność punktami
            if (pointsMethod != null) {
                BigDecimal costR4 = BigDecimalUtil.applyDiscount(order.getValue(), pointsMethod.getDiscount());
                BigDecimal discountR4 = BigDecimalUtil.calculateDiscountAmount(order.getValue(), pointsMethod.getDiscount());
                // Dodajemy opcję tylko jeśli rabat jest > 0, bo R4 może mieć 0% rabatu jeśli tak zdefiniowano,
                // a R3 daje wtedy 10%. Priorytetyzujemy rabat, więc R4 z 0% rabatu nie jest "z rabatem".
                if (discountR4.compareTo(BigDecimal.ZERO) > 0) {
                    potentialPayments.add(new PotentialFullPayment(order, pointsMethod, costR4, discountR4));
                }
            }

            // Opcje R2: Pełna płatność kwalifikującą się kartą bankową
            // Sprawdzamy, czy lista promocji nie jest null i nie jest pusta
            if (order.getPromotions() != null && !order.getPromotions().isEmpty()) {
                for (String promoId : order.getPromotions()) {
                    // Upewniamy się, że to nie jest metoda PUNKTY (punkty obsłużone jako R4)
                    // i że taka metoda płatności faktycznie istnieje
                    if (!promoId.equals(POINTS_METHOD_ID) && paymentMethodsMap.containsKey(promoId)) {
                        PaymentMethod cardMethod = paymentMethodsMap.get(promoId);
                        BigDecimal costR2 = BigDecimalUtil.applyDiscount(order.getValue(), cardMethod.getDiscount());
                        BigDecimal discountR2 = BigDecimalUtil.calculateDiscountAmount(order.getValue(), cardMethod.getDiscount());

                        // Dodajemy opcję tylko jeśli rabat jest > 0
                        if (discountR2.compareTo(BigDecimal.ZERO) > 0) {
                            potentialPayments.add(new PotentialFullPayment(order, cardMethod, costR2, discountR2));
                        }
                    }
                }
            }
        }

        // Sortujemy potencjalne płatności malejąco według rabatu
        potentialPayments.sort(Comparator.comparing(PotentialFullPayment::getDiscountAmount).reversed());

        // Przechodzimy przez posortowane opcje i aplikujemy, jeśli to możliwe
        for (PotentialFullPayment payment : potentialPayments) {
            Order order = payment.getOrder();
            PaymentMethod method = payment.getPaymentMethod();
            BigDecimal amountToPay = payment.getAmountToPay();

            // Sprawdzamy, czy zamówienie nie zostało już opłacone (mogło być opłacone przez inną opcję z tej listy)
            // i czy metoda płatności ma wystarczający limit
            if (!order.isPaid() && method.getRemainingLimit().compareTo(amountToPay) >= 0) {
                // Aplikuj płatność
                method.deductLimit(amountToPay);
                method.addSpent(amountToPay);
                order.markAsPaid(); // Oznacz zamówienie jako opłacone

                // Logowanie (opcjonalne, do debugowania)
                // System.out.println("Applied full payment for Order " + order.getId() + " with " + method.getId() + " for " + amountToPay + ". Discount: " + payment.getDiscountAmount());
            }
        }
    }

    // --- Krok 3: Alokacja pozostałych płatności (R3 i 0%) ---
    private void allocateRemainingPayments() {
        // Przechodzimy przez zamówienia, które nie zostały opłacone w kroku 2
        // Kolejność przetwarzania pozostałych zamówień może wpływać na wynik
        // (np. które zamówienie "zużyje" ostatnie punkty). Można tu dodać sortowanie.
        // Na razie przetwarzamy w oryginalnej kolejności.
        for (Order order : orders) {
            if (order.isPaid()) {
                continue;
            }

            // Zamówienie nieopłacone. Próbujemy opcje w kolejności: R3, potem 0%
            boolean paidThisOrder = false;

            // Próba opcji R3 (Częściowa płatność punktami + reszta kartą)
            // Możliwe tylko jeśli metoda PUNKTY istnieje i limit punktów pozwala na co najmniej 10% prog
            if (pointsMethod != null) {
                BigDecimal tenPercentOfValue = BigDecimalUtil.percentage(order.getValue(), MIN_POINTS_PERCENTAGE_FOR_R3.intValue()); // Procent od oryginalnej wartości przed rabatem!

                // Sprawdzamy, czy PUNKTY ma wystarczający limit na ten minimalny próg 10%
                if (pointsMethod.getRemainingLimit().compareTo(tenPercentOfValue) >= 0) {
                    BigDecimal costR3 = BigDecimalUtil.applyDiscount(order.getValue(), 10); // Rabat R3 to zawsze 10%
                    BigDecimal maxPointsForR3 = BigDecimalUtil.min(costR3, pointsMethod.getRemainingLimit()); // Ile punktów możemy maksymalnie użyć w ramach kosztu R3 i limitu PUNKTY
                    BigDecimal remainingCardPayment = costR3.subtract(maxPointsForR3); // Ile pozostaje do zapłaty kartą

                    // Musimy znaleźć dowolną kartę z wystarczającym limitem na remainingCardPayment
                    PaymentMethod cardForR3 = findCardWithSufficientLimit(remainingCardPayment);

                    if (cardForR3 != null) {
                        // Możemy zastosować R3
                        BigDecimal pointsPaymentAmount = maxPointsForR3; // Kwota punktami
                        BigDecimal cardPaymentAmount = remainingCardPayment; // Kwota kartą

                        // Aplikuj płatność
                        pointsMethod.deductLimit(pointsPaymentAmount);
                        pointsMethod.addSpent(pointsPaymentAmount);
                        cardForR3.deductLimit(cardPaymentAmount);
                        cardForR3.addSpent(cardPaymentAmount);
                        order.markAsPaid(); // Oznacz zamówienie jako opłacone
                        paidThisOrder = true;

                        // Logowanie (opcjonalne)
                        // System.out.println("Applied R3 payment for Order " + order.getId() + " with " + pointsPaymentAmount + " PUNKTY and " + cardPaymentAmount + " " + cardForR3.getId());

                    }
                }
            }

            // Jeśli zamówienie nadal nieopłacone, próbujemy opcję bazową (0% rabatu)
            if (!paidThisOrder) {
                BigDecimal fullValue = order.getValue();
                PaymentMethod cardForBase = findCardWithSufficientLimit(fullValue);

                if (cardForBase != null) {
                    // Możemy zastosować płatność bazową
                    BigDecimal paymentAmount = fullValue;

                    // Aplikuj płatność
                    cardForBase.deductLimit(paymentAmount);
                    cardForBase.addSpent(paymentAmount);
                    order.markAsPaid(); // Oznacz zamówienie jako opłacone
                    paidThisOrder = true;

                    // Logowanie (opcjonalne)
                    // System.out.println("Applied Base payment for Order " + order.getId() + " with " + cardForBase.getId() + " for " + paymentAmount);

                }
            }

            // Jeśli po próbie R3 i bazowej zamówienie nadal nie jest opłacone,
            // oznacza to, że algorytm nie znalazł sposobu z obecnymi limitami.
            // Zgodnie z wymaganiami, wszystkie zamówienia muszą być opłacone,
            // więc taki stan wskazuje na problem (np. limity są za niskie,
            // lub strategia algorytmu zawiodła dla tego przypadku).
            // Rzucamy wyjątek, bo nie spełniliśmy kluczowego wymagania.
            if (!paidThisOrder) {
                throw new RuntimeException("Could not find a payment method for Order " + order.getId() + ". Check limits or algorithm logic.");
            }
        }
    }

    // Metoda pomocnicza do znajdowania karty (nie PUNKTY) z wystarczającym limitem
    private PaymentMethod findCardWithSufficientLimit(BigDecimal amount) {
        // Jeśli kwota do zapłaty jest zero lub ujemna, nie potrzebujemy karty (zwracamy null)
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // Szukamy pierwszej dostępnej karty z wystarczającym limitem
        // Można tu dodać bardziej zaawansowaną logikę wyboru karty,
        // np. preferując karty z większym limitem, ale na razie bierzemy pierwszą znalezioną.
        return paymentMethodsMap.values().stream()
                .filter(pm -> !pm.getId().equals(POINTS_METHOD_ID)) // Tylko karty bankowe
                .filter(pm -> pm.getRemainingLimit().compareTo(amount) >= 0) // Czy limit jest wystarczający
                .findFirst() // Bierzemy pierwszą znalezioną
                .orElse(null); // Zwracamy null, jeśli nie znaleziono takiej karty
    }


    // --- Krok 4: Weryfikacja ---
    private void verifyAllOrdersPaid() {
        boolean allPaid = orders.stream().allMatch(Order::isPaid);
        if (!allPaid) {
            // To powinno być już obsłużone w allocateRemainingPayments rzucając wyjątek,
            // ale można dodać dodatkowe sprawdzenie tutaj.
            List<String> unpaidOrderIds = orders.stream()
                    .filter(order -> !order.isPaid())
                    .map(Order::getId)
                    .collect(Collectors.toList());
            throw new RuntimeException("Not all orders were paid. Unpaid orders: " + unpaidOrderIds);
        }
        // Opcjonalnie można sprawdzić, czy sumaryczne wydatki na zamówienia opłacone równają się sumarycznej wartości wszystkich zamówień po rabatach
        // ... wymaga śledzenia faktycznego rabatu przyznanego dla każdego zamówienia.
    }

    // --- Krok 5: Zbieranie wyników ---
    private List<Result> collectResults() {
        return paymentMethodsMap.values().stream()
                .filter(pm -> pm.getTotalSpent().compareTo(BigDecimal.ZERO) > 0) // Tylko metody, którymi faktycznie zapłacono
                .map(pm -> new Result(pm.getId(), pm.getTotalSpent())) // Tworzymy obiekty Result
                .collect(Collectors.toList());
        // Można tu dodać sortowanie listy Result, jeśli wymagana jest konkretna kolejność
        // .sorted(Comparator.comparing(Result::getMethodId))
    }

    // Klasa pomocnicza do przechowywania danych o potencjalnej pełnej płatności
    private static class PotentialFullPayment {
        private final Order order;
        private final PaymentMethod paymentMethod;
        private final BigDecimal amountToPay;
        private final BigDecimal discountAmount; // Ilość rabatu w PLN dla tego zamówienia tą metodą

        public PotentialFullPayment(Order order, PaymentMethod paymentMethod, BigDecimal amountToPay, BigDecimal discountAmount) {
            this.order = order;
            this.paymentMethod = paymentMethod;
            this.amountToPay = amountToPay;
            this.discountAmount = discountAmount;
        }

        public Order getOrder() { return order; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public BigDecimal getAmountToPay() { return amountToPay; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
    }
}
Use code with caution.
Java
5. Poprawki w klasie Main:
Używamy nowej klasy JsonParser do wczytania danych i PaymentOptimizer do wykonania logiki, a następnie wypisujemy wyniki. Dodajemy też podstawową obsługę błędów dla argumentów wiersza poleceń i wczytywania plików.
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