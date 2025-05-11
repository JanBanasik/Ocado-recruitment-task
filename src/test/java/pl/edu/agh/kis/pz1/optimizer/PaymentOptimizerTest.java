package pl.edu.agh.kis.pz1.optimizer;

import org.junit.jupiter.api.Test;

import pl.edu.agh.kis.pz1.domain.Order;
import pl.edu.agh.kis.pz1.domain.PaymentMethod;
import pl.edu.agh.kis.pz1.domain.Result;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


class PaymentOptimizerTest {

    private PaymentMethod createMethod(String id, int discount, String limit) {
        PaymentMethod method = new PaymentMethod(id, discount, new BigDecimal(limit), null, BigDecimal.ZERO);
        method.initializeRemainingLimit();
        return method;
    }

    private Order createOrder(String id, String value, List<String> promotions) {
        Order order = new Order(id, new BigDecimal(value), promotions, false, null);
        order.initializeRemainingValue();
        return order;
    }

    @Test
    void simpleR2DiscountApplied() throws NotFoundPaymentsException {
        List<Order> orders = Collections.singletonList(
                createOrder("ORDER1", "100.00", Collections.singletonList("CardA"))
        );
        List<PaymentMethod> methods = Collections.singletonList(
                createMethod("CardA", 10, "100.00")
        );

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);
        List<Result> results = optimizer.optimize();

        assertEquals(1, results.size());
        assertEquals(0, new BigDecimal("90.00").compareTo(results.getFirst().getAmountSpend()));
        assertEquals("CardA", results.getFirst().getMethodId());

        assertTrue(orders.getFirst().isPaid());
        assertEquals(0, BigDecimal.ZERO.compareTo(orders.getFirst().getRemainingValueToPay()));

        PaymentMethod cardA = methods.getFirst();
        assertEquals(0, new BigDecimal("90.00").compareTo(cardA.getTotalSpent()));
        assertEquals(0, new BigDecimal("10.00").compareTo(cardA.getRemainingLimit()));
    }

    @Test
    void simpleR3DiscountApplied() throws NotFoundPaymentsException {
        List<Order> orders = Collections.singletonList(
                createOrder("ORDER1", "100.00", null)
        );
        List<PaymentMethod> methods = Arrays.asList(
                createMethod("PUNKTY", 15, "50.00"),
                createMethod("CardA", 0, "100.00")
        );

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);
        List<Result> results = optimizer.optimize();

        assertEquals(2, results.size());
        Map<String, BigDecimal> spentAmounts = results.stream().collect(Collectors.toMap(Result::getMethodId, Result::getAmountSpend));

        assertEquals(0, new BigDecimal("50.00").compareTo(spentAmounts.get("PUNKTY")));
        assertEquals(0, new BigDecimal("40.00").compareTo(spentAmounts.get("CardA")));

        assertTrue(orders.getFirst().isPaid());
        assertEquals(0, BigDecimal.ZERO.compareTo(orders.getFirst().getRemainingValueToPay()));

        PaymentMethod punkty = methods.stream().filter(m -> "PUNKTY".equals(m.getId())).findFirst().get();
        PaymentMethod cardA = methods.stream().filter(m -> "CardA".equals(m.getId())).findFirst().get();
        assertEquals(0, new BigDecimal("50.00").compareTo(punkty.getTotalSpent()));
        assertEquals(0, BigDecimal.ZERO.compareTo(punkty.getRemainingLimit()));
        assertEquals(0, new BigDecimal("40.00").compareTo(cardA.getTotalSpent()));
        assertEquals(0, new BigDecimal("60.00").compareTo(cardA.getRemainingLimit()));
    }


    @Test
    void throwsExceptionTest() {
        List<Order> orders = Collections.singletonList(
                createOrder("ORDER_ZERO", "0.00", Collections.singletonList("CardA"))
        );
        List<PaymentMethod> methods = Collections.singletonList(
                createMethod("CardA", 10, "100.00")
        );

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);

        assertThrows(NotFoundPaymentsException.class, optimizer::optimize);

    }

    @Test
    void basePaymentWhenNoPromotionsAndNoPunkty() throws NotFoundPaymentsException {
        List<Order> orders = Collections.singletonList(
                createOrder("ORDER_NO_PROMO", "100.00", new ArrayList<>())
        );
        List<PaymentMethod> methods = Collections.singletonList(
                createMethod("CardA", 0, "100.00")
        );

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);
        List<Result> results = optimizer.optimize();

        assertEquals(1, results.size());
        assertEquals(0, new BigDecimal("100.00").compareTo(results.getFirst().getAmountSpend()));
        assertEquals("CardA", results.getFirst().getMethodId());

        assertTrue(orders.getFirst().isPaid());
        assertEquals(0, BigDecimal.ZERO.compareTo(orders.getFirst().getRemainingValueToPay()));

        PaymentMethod cardA = methods.getFirst();
        assertEquals(0, new BigDecimal("100.00").compareTo(cardA.getTotalSpent()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cardA.getRemainingLimit()));
    }

}