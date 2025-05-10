package pl.edu.agh.kis.pz1.optimizer;

import lombok.Getter;
import pl.edu.agh.kis.pz1.domain.Order;
import pl.edu.agh.kis.pz1.domain.PaymentMethod;
import pl.edu.agh.kis.pz1.domain.Result;
import pl.edu.agh.kis.pz1.utils.BigDecimalUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class responsible for optimally allocating payments for a list of orders
 * based on available payment methods and their limits, considering
 * promotion rules.
 * It implements a greedy algorithm prioritizing discounts.
 */
public class PaymentOptimizer {

    private final List<Order> orders;
    private final Map<String, PaymentMethod> paymentMethodsMap;
    private final PaymentMethod pointsMethod;

    private static final String POINTS_METHOD_ID = "PUNKTY";
    // Minimum percentage of the original order value that must be paid with points for promotion R3 (10% general discount)
    private static final BigDecimal MIN_POINTS_PERCENTAGE_FOR_R3 = BigDecimal.valueOf(10);

    /**
     * Creates a new instance of the payment optimizer.
     *
     * @param orders A list of orders to process.
     * @param paymentMethods A list of available payment methods.
     */
    public PaymentOptimizer(List<Order> orders, List<PaymentMethod> paymentMethods) {
        this.orders = orders;

        this.paymentMethodsMap = paymentMethods.stream()
                .collect(Collectors.toMap(PaymentMethod::getId, pm -> pm));


        this.pointsMethod = paymentMethodsMap.get(POINTS_METHOD_ID);


        if (pointsMethod == null) {
            System.err.println("Warning: Payment method '" + POINTS_METHOD_ID + "' not found. R3 and R4 promotions will not be available.");
        }

        boolean anyCardMethodExists = paymentMethods.stream()
                .anyMatch(pm -> !pm.getId().equals(POINTS_METHOD_ID));
        if (!anyCardMethodExists) {
            System.err.println("Warning: No card payment methods found. Only PUNKTY payments are possible if available.");
        }
    }

    /**
     * Executes the payment optimization process, allocating available funds
     * for each order according to the adopted greedy strategy.
     * Upon completion, all orders should be paid,
     * and the method returns the total amounts spent per payment method.
     *
     * @return A list of Result objects containing the total amounts spent on each payment method.
     * @throws RuntimeException if not all orders can be paid within the available limits
     *                          and the adopted allocation strategy.
     */
    public List<Result> optimize() throws NotFoundPaymentsException {

        paymentMethodsMap.values().forEach(pm -> pm.setTotalSpent(BigDecimal.ZERO)); // Reset total spent amounts

        // Account for only the most rewarding promotions
        allocateFullPaymentsWithDiscount();

        // If we didn't find match for an order, we can try remaining promotions
        allocateRemainingPayments();

        // If not, throw an exception.
        verifyAllOrdersPaid();

        return collectResults();
    }

    /**
     * Private method allocating full payments for orders that offer
     * an additional discount (R2 and R4 promotions).
     * Potential payments are sorted in descending order by discount value
     * and applied in that order, if limits allow.
     * Modifies the state of Order and PaymentMethod objects.
     */
    private void allocateFullPaymentsWithDiscount() {

        List<PotentialFullPayment> potentialPayments = new ArrayList<>();

        for (Order order : orders) {

            if (order.isPaid()) {
                continue;
            }

            // Full payment with points (if PUNKTY exists and offers a discount > 0)
            findFullPaymentWithPoints(order, potentialPayments);

            // Full payment with a qualifying bank card (if the promotions list is not null/empty)
            findfullPaymentWithCard(order, potentialPayments);
        }

        // The greedy algorithm attempts to apply the most favorable discounts first.
        potentialPayments.sort(Comparator.comparing(PotentialFullPayment::getDiscountAmount).reversed());

        // Iterate through the sorted options and apply the payment if the order has not been paid yet
        // and the payment method has a sufficient limit.
        for (PotentialFullPayment payment : potentialPayments) {
            Order order = payment.getOrder();
            PaymentMethod method = payment.getPaymentMethod();
            BigDecimal amountToPay = payment.getAmountToPay();

            if (!order.isPaid() && method.getRemainingLimit().compareTo(amountToPay) >= 0) {
                method.deductLimit(amountToPay);
                method.addSpent(amountToPay);
                order.markAsPaid();
            }
        }
    }

    private void findFullPaymentWithPoints(Order order, List<PotentialFullPayment> potentialPayments) {
        if (pointsMethod != null) {
            BigDecimal costR4 = BigDecimalUtil.applyDiscount(order.getValue(), pointsMethod.getDiscount());
            BigDecimal discountR4 = BigDecimalUtil.calculateDiscountAmount(order.getValue(), pointsMethod.getDiscount());

            if (discountR4.compareTo(BigDecimal.ZERO) > 0) {
                potentialPayments.add(new PotentialFullPayment(order, pointsMethod, costR4, discountR4));
            }
        }
    }

    private void findfullPaymentWithCard(Order order, List<PotentialFullPayment> potentialPayments) {
        if (order.getPromotions() != null && !order.getPromotions().isEmpty()) {
            for (String promoId : order.getPromotions()) {

                if (!promoId.equals(POINTS_METHOD_ID) && paymentMethodsMap.containsKey(promoId)) {
                    PaymentMethod cardMethod = paymentMethodsMap.get(promoId);
                    BigDecimal costR2 = BigDecimalUtil.applyDiscount(order.getValue(), cardMethod.getDiscount());
                    BigDecimal discountR2 = BigDecimalUtil.calculateDiscountAmount(order.getValue(), cardMethod.getDiscount());

                    if (discountR2.compareTo(BigDecimal.ZERO) > 0) {
                        potentialPayments.add(new PotentialFullPayment(order, cardMethod, costR2, discountR2));
                    }
                }
            }
        }
    }

    /**
     * Private method handling remaining, unpaid orders.
     * It attempts to apply the R3 discount (10% for paying >= 10% of value with points)
     * or, if R3 is not possible, the base payment without discount (0%)
     * using any available card.
     * Modifies the state of Order and PaymentMethod objects.
     * Throws an exception if a payment method cannot be found for an order.
     */
    private void allocateRemainingPayments() throws NotFoundPaymentsException {

        for (Order order : orders) {
            if (order.isPaid()) {
                continue;
            }

            boolean paidThisOrder = false;

            // Partial payment with points + remainder with card
            paidThisOrder = canPayPartiallyWithPoints(order, paidThisOrder);

            if (!paidThisOrder) {
                BigDecimal fullValue = order.getValue(); // Full order value, because no discount.

                PaymentMethod cardForBase = findCardWithSufficientLimit(fullValue);

                if (cardForBase != null) {
                    cardForBase.deductLimit(fullValue);
                    cardForBase.addSpent(fullValue);
                    order.markAsPaid();
                    paidThisOrder = true;
                }
            }

            // If order remains unpaid, algorithm failed to find correct payment match
            if (!paidThisOrder) {
                throw new NotFoundPaymentsException("Could not find a payment method for Order " + order.getId() + ". Check available payment methods limits or algorithm logic.");
            }
        }
    }

    private boolean canPayPartiallyWithPoints(Order order, boolean paidThisOrder) {
        if (pointsMethod != null) {
            // Calculate 10% of the original order value - the threshold for R3.
            BigDecimal tenPercentOfValue = BigDecimalUtil.percentage(order.getValue(), MIN_POINTS_PERCENTAGE_FOR_R3.intValue());

            // Check if PUNKTY has a sufficient limit for this minimum 10% threshold
            if (pointsMethod.getRemainingLimit().compareTo(tenPercentOfValue) >= 0) {
                // We can apply discount then
                BigDecimal costR3 = BigDecimalUtil.applyDiscount(order.getValue(), 10);
                // Calculate the maximum possible amount to pay with points within the R3 cost
                // and the available points limit (we prefer points in R3).
                BigDecimal maxPointsForR3 = BigDecimalUtil.min(costR3, pointsMethod.getRemainingLimit());
                // Calculate the remaining amount that needs to be paid with a card.
                BigDecimal remainingCardPayment = costR3.subtract(maxPointsForR3);

                PaymentMethod cardForR3 = findCardWithSufficientLimit(remainingCardPayment);

                // If PUNKTY is available for the >= 10% threshold AND a card is found for the remainder:
                if (cardForR3 != null) {
                    pointsMethod.deductLimit(maxPointsForR3);
                    pointsMethod.addSpent(maxPointsForR3);
                    cardForR3.deductLimit(remainingCardPayment);
                    cardForR3.addSpent(remainingCardPayment);
                    order.markAsPaid();
                    paidThisOrder = true;
                }
            }
        }
        return paidThisOrder;
    }

    /**
     * Private helper method to find the first available payment method
     * that is a card (other than PUNKTY) with a sufficient remaining limit
     * to cover the given amount.
     *
     * @param amount The amount for which we are looking for a card with a sufficient limit.
     * @return A PaymentMethod object representing the found card, or {@code null} if no matching card is found.
     */
    private PaymentMethod findCardWithSufficientLimit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return paymentMethodsMap.values().stream()
                .filter(pm -> !pm.getId().equals(POINTS_METHOD_ID))
                .filter(pm -> pm.getRemainingLimit().compareTo(amount) >= 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * Private method verifying that all orders have been successfully
     * marked as paid after the allocation processes are complete.
     *
     * @throws RuntimeException if at least one order has not been paid.
     */
    private void verifyAllOrdersPaid() throws NotFoundPaymentsException {
        boolean allPaid = orders.stream().allMatch(Order::isPaid);
        if (!allPaid) {
            List<String> unpaidOrderIds = orders.stream()
                    .filter(order -> !order.isPaid())
                    .map(Order::getId)
                    .toList();
            throw new NotFoundPaymentsException("Not all orders were paid successfully after optimization attempt. Unpaid orders: " + unpaidOrderIds);
        }
    }

    /**
     * Private method collecting the total amounts spent on each payment method
     * that was actually used.
     *
     * @return A list of Result objects containing the method ID and the total spent amount (formatted).
     */
    private List<Result> collectResults() {

        return paymentMethodsMap.values().stream()
                // Filter only methods for which the total spent amount is greater than zero.
                .filter(pm -> pm.getTotalSpent().compareTo(BigDecimal.ZERO) > 0)
                // Map each PaymentMethod object to a new Result object,
                // using the method ID and the total spent amount.
                .map(pm -> new Result(pm.getId(), pm.getTotalSpent()))
                .toList();
    }

    /**
     * Private static helper class for storing data about a potential full payment
     * for a specific order in the discount payment allocation step (R2 and R4).
     */
    @Getter
    private static class PotentialFullPayment {

        private final Order order;
        private final PaymentMethod paymentMethod;
        private final BigDecimal amountToPay;
        private final BigDecimal discountAmount;

        /**
         * Creates a new instance of PotentialFullPayment.
         *
         * @param order          The order concerned by the potential payment.
         * @param paymentMethod  The payment method proposed for this order.
         * @param amountToPay    The amount that needs to be paid with this method (after discount).
         * @param discountAmount The discount amount obtained by this payment.
         */
        public PotentialFullPayment(Order order, PaymentMethod paymentMethod, BigDecimal amountToPay, BigDecimal discountAmount) {
            this.order = order;
            this.paymentMethod = paymentMethod;
            this.amountToPay = amountToPay;
            this.discountAmount = discountAmount;
        }
    }
}