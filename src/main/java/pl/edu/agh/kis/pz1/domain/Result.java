package pl.edu.agh.kis.pz1.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents the final spending amount for a specific payment method.
 * Used to summarize the results of the payment optimization.
 */
@Getter
@AllArgsConstructor
public class Result {

    /**
     * The ID of the payment method.
     */
    private String methodId;

    /**
     * The total amount spent using this payment method.
     */
    private BigDecimal amountSpend;

    /**
     * Returns a string representation of the Result object, formatted
     * as "methodId amountSpend" with the amount rounded to two decimal places.
     *
     * @return A formatted string representing the spending for the payment method.
     */
    @Override
    public String toString() {
        // Format the amount to two decimal places using HALF_UP rounding
        return methodId + " " + amountSpend.setScale(2, RoundingMode.HALF_UP);
    }
}