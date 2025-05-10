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
            return null; // or BigDecimal.ZERO, depending on needs
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
        if (value == null || percent < 0) {
            // Handle errors or return zero
            return BigDecimal.ZERO;
        }

        BigDecimal percentDecimal = BigDecimal.valueOf(percent);
        BigDecimal hundred = BigDecimal.valueOf(100);

        // Calculate value * percent / 100. Use BigDecimal.valueOf(percent) and 100
        // Multiply first, then divide to maintain precision before final scaling.
        // We divide with a slightly larger scale temporarily...
        BigDecimal result = value.multiply(percentDecimal).divide(hundred, SCALE + 2, ROUNDING_MODE);
        // ...and then set the final scale.
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
            // Error handling or return original value
            return value;
        }
        BigDecimal percentToPay = BigDecimal.valueOf(100 - percent);
        BigDecimal hundred = BigDecimal.valueOf(100);
        // Similar to percentage, use intermediate scale before final scaling
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
     * Adds two BigDecimal values and returns the result with the defined scale and rounding.
     * Treats null values as zero.
     *
     * @param a The first BigDecimal operand.
     * @param b The second BigDecimal operand.
     * @return The sum of a and b, scaled and rounded.
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return setScale(a.add(b));
    }

    /**
     * Subtracts the second BigDecimal value from the first and returns the result with the defined scale and rounding.
     * Treats null values as zero.
     *
     * @param a The first BigDecimal operand (minuend).
     * @param b The second BigDecimal operand (subtrahend).
     * @return The result of a minus b, scaled and rounded.
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return setScale(a.subtract(b));
    }

    /**
     * Multiplies two BigDecimal values and returns the result with the defined scale and rounding.
     * Returns BigDecimal.ZERO if either operand is null.
     *
     * @param a The first BigDecimal operand.
     * @param b The second BigDecimal operand.
     * @return The product of a and b, scaled and rounded. Returns BigDecimal.ZERO if a or b is null.
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return BigDecimal.ZERO;
        // Multiplication usually doesn't require intermediate rounding, only the final scale.
        return setScale(a.multiply(b));
    }

    /**
     * Divides the first BigDecimal value by the second and returns the result with the defined scale and rounding.
     * Throws ArithmeticException if the divisor is zero or null.
     *
     * @param a The first BigDecimal operand (dividend).
     * @param b The second BigDecimal operand (divisor).
     * @return The result of a divided by b, scaled and rounded.
     * @throws ArithmeticException if b is null or BigDecimal.ZERO.
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || b.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero or null operand");
        }
        // Perform division keeping a higher precision before final scaling
        // to avoid intermediate rounding errors. Adding 4 extra places for safety.
        return setScale(a.divide(b, SCALE + 4, ROUNDING_MODE));
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

    /**
     * Returns the maximum of two BigDecimal values.
     * If one is null, returns the other. If both are null, behavior is undefined (consider adding null checks if needed).
     *
     * @param a The first BigDecimal value.
     * @param b The second BigDecimal value.
     * @return The larger of a and b. Returns b if a is null, a if b is null.
     */
    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.max(b);
    }
}