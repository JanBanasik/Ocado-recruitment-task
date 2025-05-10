package pl.edu.agh.kis.pz1.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for performing common operations on BigDecimal with a fixed scale
 * and rounding mode, specifically for currency calculations.
 * Prevents common floating-point precision issues.
 */
public class BigDecimalUtil {

    /**
     * Standard scale for currency values (2 decimal places).
     */
    public static final int SCALE = 2;

    /**
     * Standard rounding mode (rounding up for 0.5 and higher).
     */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private BigDecimalUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Sets the defined scale and rounding mode for a BigDecimal value.
     * Returns null if the input value is null.
     *
     * @param value The BigDecimal value to scale and round.
     * @return The scaled and rounded BigDecimal value, or null if input is null.
     */
    public static BigDecimal setScale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates a percentage of a BigDecimal value.
     * Example: percentage(BigDecimal.valueOf(100), 10) returns 10.00.
     *
     * @param value The base BigDecimal value.
     * @param percent The percentage as an integer (e.g., 10 for 10%). Must be non-negative.
     * @return The calculated percentage amount as a BigDecimal, rounded to the defined scale. Returns BigDecimal.ZERO if value is null or percent is negative.
     */
    public static BigDecimal percentage(BigDecimal value, int percent) {
        if (value == null || percent < 0 || percent > 100) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentDecimal = BigDecimal.valueOf(percent);
        BigDecimal hundred = BigDecimal.valueOf(100);

        BigDecimal result = value.multiply(percentDecimal).divide(hundred, SCALE + 2, ROUNDING_MODE);
        return setScale(result);
    }

    /**
     * Calculates the value of an amount after applying a percentage discount.
     * Example: applyDiscount(BigDecimal.valueOf(100), 10) returns 90.00.
     *
     * @param value The original BigDecimal value.
     * @param percent The discount percentage as an integer (e.g., 10 for 10%). Must be between 0 and 100.
     * @return The discounted amount as a BigDecimal, rounded to the defined scale. Returns the original value if input is null or percent is out of bounds.
     */
    public static BigDecimal applyDiscount(BigDecimal value, int percent) {
        if (value == null || percent < 0 || percent > 100) {
            return value;
        }
        BigDecimal percentToPay = BigDecimal.valueOf(100L - percent);
        BigDecimal hundred = BigDecimal.valueOf(100);

        BigDecimal result = value.multiply(percentToPay).divide(hundred, SCALE + 2, ROUNDING_MODE);
        return setScale(result);
    }

    /**
     * Calculates the absolute amount of a discount given an original value and percentage.
     * Example: calculateDiscountAmount(BigDecimal.valueOf(100), 10) returns 10.00.
     *
     * @param originalValue The original BigDecimal value.
     * @param percent The discount percentage as an integer (e.g., 10 for 10%). Must be between 0 and 100.
     * @return The discount amount as a BigDecimal, rounded to the defined scale. Returns BigDecimal.ZERO if input is null or percent is out of bounds.
     */
    public static BigDecimal calculateDiscountAmount(BigDecimal originalValue, int percent) {
        if (originalValue == null || percent < 0 || percent > 100) {
            return BigDecimal.ZERO;
        }
        BigDecimal discountedValue = applyDiscount(originalValue, percent);
        // Subtracting the discounted value from the original value gives the discount amount.
        return originalValue.subtract(discountedValue);
    }

    /**
     * Returns the minimum of two BigDecimal values.
     * If one is null, returns the other. If both are null, behavior is undefined (consider adding null checks if needed).
     *
     * @param a The first BigDecimal value.
     * @param b The second BigDecimal value.
     * @return The smaller of a and b. Returns b if a is null, a if b is null.
     */
    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.min(b);
    }

}