package pl.edu.agh.kis.pz1.domain;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a customer order.
 * Includes details such as order ID, total value, applicable promotions,
 * and tracking fields for payment status and remaining value to be paid.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * The unique identifier of the order.
     */
    private String id;

    /**
     * The total value of the order before any discounts are applied.
     */
    private BigDecimal value;

    /**
     * An optional list of payment method IDs that are eligible for promotion R2
     * when used for full payment of this order.
     * If null or empty, only R3/R4 promotions are applicable.
     */
    private List<String> promotions;

    /**
     * Flag indicating whether the order has been fully paid.
     * Initialized to false.
     */
    private boolean isPaid = false;

    /**
     * The remaining amount that still needs to be paid for this order.
     * Initially set to the order's full value. Should be zero when isPaid is true.
     */
    private BigDecimal remainingValueToPay;

    /**
     * Initializes the {@code remainingValueToPay} to the order's full {@code value}.
     * This should be called after parsing or creating a new Order object
     * before starting payment allocation.
     */
    public void initializeRemainingValue() {
        this.remainingValueToPay = this.value;
    }

    /**
     * Marks the order as paid and sets the {@code remainingValueToPay} to zero.
     * This should be called when the full amount for the order (after any discounts)
     * has been successfully allocated.
     */
    public void markAsPaid() {
        this.isPaid = true;
        this.remainingValueToPay = BigDecimal.ZERO; // Ensure remaining value is zero
    }
}