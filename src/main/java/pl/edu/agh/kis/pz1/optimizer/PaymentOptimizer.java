package pl.edu.agh.kis.pz1.optimizer;

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
    private final PaymentMethod pointsMethod; // Convenient reference to the PUNKTY method

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
        // Convert the list of payment methods to a map for easy access by ID
        this.paymentMethodsMap = paymentMethods.stream()
                .collect(Collectors.toMap(PaymentMethod::getId, pm -> pm));

        // Get a reference to the PUNKTY method
        this.pointsMethod = paymentMethodsMap.get(POINTS_METHOD_ID);

        // Check if the PUNKTY method exists (required for R3 and R4)
        if (pointsMethod == null) {
            System.err.println("Warning: Payment method '" + POINTS_METHOD_ID + "' not found. R3 and R4 promotions will not be available.");
            // Do not throw an error, but these payment options will not be considered in the algorithm.
        }

        // Additional check: are there any other payment methods (cards)?
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
    public List<Result> optimize() {
        // Step 1: Initialize states in Order and PaymentMethod objects
        // These initializations are also done in JsonParser, but repeating ensures a clean state before optimization.
        orders.forEach(Order::initializeRemainingValue);
        paymentMethodsMap.values().forEach(PaymentMethod::initializeRemainingLimit);
        paymentMethodsMap.values().forEach(pm -> pm.setTotalSpent(BigDecimal.ZERO)); // Reset total spent amounts

        // Step 2: Allocate full payments with the largest discount (R2 and R4) first.
        // This is part of the greedy strategy aiming to maximize the total discount.
        allocateFullPaymentsWithDiscount();

        // Step 3: Handle remaining orders, attempting to apply the R3 discount first,
        // and then the base payment (0% discount).
        allocateRemainingPayments();

        // Step 4: Verify that all orders have been successfully paid.
        // If not, throw an exception.
        verifyAllOrdersPaid();

        // Step 5: Collect the total amounts spent for each payment method that was actually used.
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
        // List of potential full payments with a discount
        List<PotentialFullPayment> potentialPayments = new ArrayList<>();

        for (Order order : orders) {
            // If the order is already paid, skip
            if (order.isPaid()) {
                continue;
            }

            // Option R4: Full payment with points (if PUNKTY exists and offers a discount > 0)
            if (pointsMethod != null) {
                BigDecimal costR4 = BigDecimalUtil.applyDiscount(order.getValue(), pointsMethod.getDiscount());
                BigDecimal discountR4 = BigDecimalUtil.calculateDiscountAmount(order.getValue(), pointsMethod.getDiscount());

                // Add the R4 option only if it offers a positive discount.
                // An R4 with 0% discount is less favorable than a potential R3 (10% discount).
                if (discountR4.compareTo(BigDecimal.ZERO) > 0) {
                    potentialPayments.add(new PotentialFullPayment(order, pointsMethod, costR4, discountR4));
                }
            }

            // Option R2: Full payment with a qualifying bank card (if the promotions list is not null/empty)
            if (order.getPromotions() != null && !order.getPromotions().isEmpty()) {
                for (String promoId : order.getPromotions()) {
                    // Make sure it's not the PUNKTY method (points handled as R4)
                    // and that such a payment method actually exists in the customer's wallet.
                    if (!promoId.equals(POINTS_METHOD_ID) && paymentMethodsMap.containsKey(promoId)) {
                        PaymentMethod cardMethod = paymentMethodsMap.get(promoId);
                        BigDecimal costR2 = BigDecimalUtil.applyDiscount(order.getValue(), cardMethod.getDiscount());
                        BigDecimal discountR2 = BigDecimalUtil.calculateDiscountAmount(order.getValue(), cardMethod.getDiscount());

                        // Add the R2 option only if it offers a positive discount.
                        if (discountR2.compareTo(BigDecimal.ZERO) > 0) {
                            potentialPayments.add(new PotentialFullPayment(order, cardMethod, costR2, discountR2));
                        }
                    }
                }
            }
        }

        // Sort potential payments in descending order by discount amount.
        // The greedy algorithm attempts to apply the most favorable discounts first.
        potentialPayments.sort(Comparator.comparing(PotentialFullPayment::getDiscountAmount).reversed());

        // Iterate through the sorted options and apply the payment if the order has not been paid yet
        // and the payment method has a sufficient limit.
        for (PotentialFullPayment payment : potentialPayments) {
            Order order = payment.getOrder();
            PaymentMethod method = payment.getPaymentMethod();
            BigDecimal amountToPay = payment.getAmountToPay();

            // Condition for applying payment: order unpaid AND sufficient limit
            if (!order.isPaid() && method.getRemainingLimit().compareTo(amountToPay) >= 0) {
                // Apply payment: update method limits and spent amounts, mark the order as paid.
                method.deductLimit(amountToPay);
                method.addSpent(amountToPay);
                order.markAsPaid();

                // Logging (optional, for debugging)
                // System.out.println("Applied full payment for Order " + order.getId() + " with " + method.getId() + " for " + amountToPay + ". Discount: " + payment.getDiscountAmount());
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
    private void allocateRemainingPayments() {
        // Iterate through orders that were not paid in the previous step.
        // The order of processing remaining orders might affect the result,
        // but for now, we process in the original order.
        for (Order order : orders) {
            // Skip orders that have already been paid.
            if (order.isPaid()) {
                continue;
            }

            // The order is unpaid. We try options in order: R3, then base (0%).
            boolean paidThisOrder = false;

            // Attempt Option R3 (Partial payment with points + remainder with card)
            // Only possible if the PUNKTY method exists
            if (pointsMethod != null) {
                // Calculate 10% of the original order value - the threshold for R3.
                BigDecimal tenPercentOfValue = BigDecimalUtil.percentage(order.getValue(), MIN_POINTS_PERCENTAGE_FOR_R3.intValue());

                // Check if PUNKTY has a sufficient limit for this minimum 10% threshold
                if (pointsMethod.getRemainingLimit().compareTo(tenPercentOfValue) >= 0) {
                    // Calculate the total cost of the order after the R3 discount (always 10% from original value).
                    BigDecimal costR3 = BigDecimalUtil.applyDiscount(order.getValue(), 10);
                    // Calculate the maximum possible amount to pay with points within the R3 cost
                    // and the available points limit (we prefer points in R3).
                    BigDecimal maxPointsForR3 = BigDecimalUtil.min(costR3, pointsMethod.getRemainingLimit());
                    // Calculate the remaining amount that needs to be paid with a card.
                    BigDecimal remainingCardPayment = costR3.subtract(maxPointsForR3);

                    // We need to find any card (not PUNKTY) with a sufficient limit for remainingCardPayment.
                    PaymentMethod cardForR3 = findCardWithSufficientLimit(remainingCardPayment);

                    // If PUNKTY is available for the >= 10% threshold AND a card is found for the remainder:
                    if (cardForR3 != null) {
                        // We can apply the R3 payment.
                        BigDecimal pointsPaymentAmount = maxPointsForR3; // Amount paid with points
                        BigDecimal cardPaymentAmount = remainingCardPayment; // Amount paid with card

                        // Apply payment: update limits and spent amounts for both payment methods, mark the order as paid.
                        pointsMethod.deductLimit(pointsPaymentAmount);
                        pointsMethod.addSpent(pointsPaymentAmount);
                        cardForR3.deductLimit(cardPaymentAmount);
                        cardForR3.addSpent(cardPaymentAmount);
                        order.markAsPaid();
                        paidThisOrder = true; // Mark that the order was paid in this round.

                        // Logging (optional)
                        // System.out.println("Applied R3 payment for Order " + order.getId() + " with " + pointsPaymentAmount + " PUNKTY and " + cardPaymentAmount + " " + cardForR3.getId());
                    }
                }
            }

            // If the order is still unpaid after attempting R3, we try the base option (0% discount).
            if (!paidThisOrder) {
                BigDecimal fullValue = order.getValue(); // Full order value, because no discount.
                // We need to find any card (not PUNKTY) with a sufficient limit for the full value.
                PaymentMethod cardForBase = findCardWithSufficientLimit(fullValue);

                if (cardForBase != null) {
                    // We can apply the base payment.
                    BigDecimal paymentAmount = fullValue;

                    // Apply payment: update the card's limit and spent amount, mark the order as paid.
                    cardForBase.deductLimit(paymentAmount);
                    cardForBase.addSpent(paymentAmount);
                    order.markAsPaid();
                    paidThisOrder = true; // Mark that the order was paid.

                    // Logging (optional)
                    // System.out.println("Applied Base payment for Order " + order.getId() + " with " + cardForBase.getId() + " for " + paymentAmount);
                }
            }

            // If after attempting all options (R3 and base) the order is still unpaid,
            // it means the algorithm could not find a way with the current limits.
            // According to the requirements, all orders must be paid for a complete solution.
            // This state indicates an issue of non-feasibility (or a limitation of the algorithm strategy).
            // We throw an exception to signal that a full solution was not found.
            if (!paidThisOrder) {
                throw new RuntimeException("Could not find a payment method for Order " + order.getId() + ". Check available payment methods limits or algorithm logic.");
            }
        }
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
        // If the amount to pay is zero or negative, we don't need a card, so return null.
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // Look for the first payment method that is not points
        // and whose remaining limit is greater than or equal to the required amount.
        // The search order depends on the stream() implementation and the order in paymentMethodsMap.
        // For simplicity, we take the first one found.
        return paymentMethodsMap.values().stream()
                .filter(pm -> !pm.getId().equals(POINTS_METHOD_ID)) // Filter to include only bank cards.
                .filter(pm -> pm.getRemainingLimit().compareTo(amount) >= 0) // Check if the limit is sufficient.
                .findFirst() // Take the first matching method.
                .orElse(null); // Return null if the stream is empty (no card found).
    }

    /**
     * Private method verifying that all orders have been successfully
     * marked as paid after the allocation processes are complete.
     *
     * @throws RuntimeException if at least one order has not been paid.
     */
    private void verifyAllOrdersPaid() {
        boolean allPaid = orders.stream().allMatch(Order::isPaid);
        if (!allPaid) {
            // Get the IDs of unpaid orders for a better error message.
            List<String> unpaidOrderIds = orders.stream()
                    .filter(order -> !order.isPaid())
                    .map(Order::getId)
                    .collect(Collectors.toList());
            // Throw an exception, signaling the inability to pay all orders.
            throw new RuntimeException("Not all orders were paid successfully after optimization attempt. Unpaid orders: " + unpaidOrderIds);
        }
        // If all are paid, the method finishes without throwing an exception.
    }

    /**
     * Private method collecting the total amounts spent on each payment method
     * that was actually used.
     *
     * @return A list of Result objects containing the method ID and the total spent amount (formatted).
     */
    private List<Result> collectResults() {
        // Create a stream from the values of the paymentMethodsMap (i.e., PaymentMethod objects).
        return paymentMethodsMap.values().stream()
                // Filter only methods for which the total spent amount is greater than zero.
                .filter(pm -> pm.getTotalSpent().compareTo(BigDecimal.ZERO) > 0)
                // Map each PaymentMethod object to a new Result object,
                // using the method ID and the total spent amount.
                .map(pm -> new Result(pm.getId(), pm.getTotalSpent()))
                // Collect the results into a new list.
                .collect(Collectors.toList());
        // Optional: can add sorting to the result list, e.g., alphabetically by method ID:
        // .sorted(Comparator.comparing(Result::getMethodId))
    }

    /**
     * Private static helper class for storing data about a potential full payment
     * for a specific order in the discount payment allocation step (R2 and R4).
     */
    private static class PotentialFullPayment {
        private final Order order;
        private final PaymentMethod paymentMethod;
        private final BigDecimal amountToPay; // Amount to pay after discount
        private final BigDecimal discountAmount; // Discount amount in PLN for this option

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

        // Getters for fields (generated by Lombok, but added manual Javadoc for clarity)

        /**
         * Returns the order concerned by the potential payment.
         * @return The Order object.
         */
        public Order getOrder() { return order; }
        /**
         * Returns the payment method proposed for this order.
         * @return The PaymentMethod object.
         */
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        /**
         * Returns the amount to pay after discount.
         * @return The amount to pay in PLN.
         */
        public BigDecimal getAmountToPay() { return amountToPay; }
        /**
         * Returns the discount amount obtained by this payment.
         * @return The discount amount in PLN.
         */
        public BigDecimal getDiscountAmount() { return discountAmount; }
    }
}