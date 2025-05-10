package pl.edu.agh.kis.pz1.domain;

import lombok.*;
import java.math.BigDecimal;

/**
 * Represents a payment method available to the customer.
 * Includes details like ID, discount percentage, total limit,
 * remaining limit, and total amount spent using this method.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    /**
     * The unique identifier of the payment method (e.g., "PUNKTY", "mZysk").
     */
    private String id;

    /**
     * The discount percentage offered by this payment method as an integer (e.g., 15 for 15%).
     */
    private int discount;

    /**
     * The total initial limit of funds available for this payment method.
     */
    private BigDecimal limit;

    /**
     * The remaining limit of funds currently available for this payment method.
     * This value decreases as payments are made using this method.
     */
    private BigDecimal remainingLimit;

    /**
     * The total cumulative amount spent using this payment method across all orders.
     * Initialized to BigDecimal.ZERO.
     */
    private BigDecimal totalSpent = BigDecimal.ZERO;

    /**
     * Initializes the {@code remainingLimit} to the full {@code limit} amount.
     * This should be called after parsing or creating a new PaymentMethod object
     * before starting payment allocation.
     */
    public void initializeRemainingLimit() {
        this.remainingLimit = this.limit;
    }

    /**
     * Adds a specified amount to the {@code totalSpent} for this payment method.
     * Ensures the amount is non-negative.
     *
     * @param amount The amount spent to add to the total.
     */
    public void addSpent(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }
        this.totalSpent = this.totalSpent.add(amount);
    }

    /**
     * Deducts a specified amount from the {@code remainingLimit} for this payment method.
     * Ensures the amount is non-negative.
     *
     * @param amount The amount to deduct from the remaining limit.
     */
    public void deductLimit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }
        this.remainingLimit = this.remainingLimit.subtract(amount);
    }
}