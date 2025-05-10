package pl.edu.agh.kis.pz1.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BigDecimalUtilTest {

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        // Use compareTo for equality check as per BigDecimal recommendations
        assertNotNull(actual, message + " - actual value is null");
        assertNotNull(expected, message + " - expected value is null");
        assertEquals(0, expected.compareTo(actual), message + " - values do not match");
    }

    @Test
    @DisplayName("setScale should correctly scale and round a BigDecimal value")
    void setScale_shouldScaleAndRound() {
        BigDecimal value = new BigDecimal("123.4567");
        BigDecimal expected = new BigDecimal("123.46");
        assertBigDecimalEquals(expected, BigDecimalUtil.setScale(value), "Scaling 123.4567");

        BigDecimal valueExact = new BigDecimal("123.45");
        BigDecimal expectedExact = new BigDecimal("123.45");
        assertBigDecimalEquals(expectedExact, BigDecimalUtil.setScale(valueExact), "Scaling 123.45");

        BigDecimal valueRoundDown = new BigDecimal("123.4549");
        BigDecimal expectedRoundDown = new BigDecimal("123.45");
        assertBigDecimalEquals(expectedRoundDown, BigDecimalUtil.setScale(valueRoundDown), "Scaling 123.4549");

        BigDecimal valueRoundUp = new BigDecimal("123.455");
        BigDecimal expectedRoundUp = new BigDecimal("123.46");
        assertBigDecimalEquals(expectedRoundUp, BigDecimalUtil.setScale(valueRoundUp), "Scaling 123.455");
    }

    @Test
    @DisplayName("setScale should return null for null input")
    void setScale_shouldReturnNullForNull() {
        assertNull(BigDecimalUtil.setScale(null), "Scaling null");
    }

    @Test
    @DisplayName("percentage should calculate percentage correctly")
    void percentage_shouldCalculateCorrectly() {
        BigDecimal value = BigDecimal.valueOf(200.00);
        BigDecimal expected10Percent = BigDecimal.valueOf(20.00);
        assertBigDecimalEquals(expected10Percent, BigDecimalUtil.percentage(value, 10), "10% of 200");

        BigDecimal expected0Percent = BigDecimal.valueOf(0.00);
        assertBigDecimalEquals(expected0Percent, BigDecimalUtil.percentage(value, 0), "0% of 200");

        BigDecimal expected100Percent = BigDecimal.valueOf(200.00);
        assertBigDecimalEquals(expected100Percent, BigDecimalUtil.percentage(value, 100), "100% of 200");

        BigDecimal valueWithDecimals = new BigDecimal("150.50");
        BigDecimal expected25Percent = new BigDecimal("37.63"); // 150.50 * 0.25 = 37.625 -> 37.63 (HALF_UP)
        // Use a slightly higher precision for expected if needed to match the internal calculation precision before final scale
        BigDecimal expected25PercentCalc = new BigDecimal("37.625").setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE);
        assertBigDecimalEquals(expected25PercentCalc, BigDecimalUtil.percentage(valueWithDecimals, 25), "25% of 150.50");
    }

    @Test
    @DisplayName("percentage should handle zero and null input values")
    void percentage_shouldHandleZeroAndNullValue() {
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.percentage(BigDecimal.ZERO, 10), "10% of 0");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.percentage(null, 10), "10% of null");
    }

    @Test
    @DisplayName("percentage should return zero for negative percentage")
    void percentage_shouldReturnZeroForNegativePercent() {
        BigDecimal value = BigDecimal.valueOf(100.00);
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.percentage(value, -5), "Negative percentage");
    }


    @Test
    @DisplayName("applyDiscount should calculate value after discount correctly")
    void applyDiscount_shouldCalculateCorrectly() {
        BigDecimal value = BigDecimal.valueOf(200.00);
        BigDecimal expected10PercentOff = BigDecimal.valueOf(180.00); // 200 * (100-10)/100
        assertBigDecimalEquals(expected10PercentOff, BigDecimalUtil.applyDiscount(value, 10), "200 after 10% off");

        BigDecimal expected0PercentOff = BigDecimal.valueOf(200.00); // 200 * (100-0)/100
        assertBigDecimalEquals(expected0PercentOff, BigDecimalUtil.applyDiscount(value, 0), "200 after 0% off");

        BigDecimal expected100PercentOff = BigDecimal.valueOf(0.00); // 200 * (100-100)/100
        assertBigDecimalEquals(expected100PercentOff, BigDecimalUtil.applyDiscount(value, 100), "200 after 100% off");

        BigDecimal valueWithDecimals = new BigDecimal("150.50");
        BigDecimal expected25PercentOff = new BigDecimal("112.88"); // 150.50 * 0.75 = 112.875 -> 112.88 (HALF_UP)
        BigDecimal expected25PercentOffCalc = new BigDecimal("112.875").setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE);
        assertBigDecimalEquals(expected25PercentOffCalc, BigDecimalUtil.applyDiscount(valueWithDecimals, 25), "150.50 after 25% off");
    }

    @Test
    @DisplayName("applyDiscount should handle zero and null input values")
    void applyDiscount_shouldHandleZeroAndNullValue() {
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.applyDiscount(BigDecimal.ZERO, 10), "0 after 10% off");
        assertNull(BigDecimalUtil.applyDiscount(null, 10), "null after 10% off"); // applyDiscount returns original value if null input
    }

    @Test
    @DisplayName("applyDiscount should handle negative or >100 percentage")
    void applyDiscount_shouldHandleInvalidPercent() {
        BigDecimal value = BigDecimal.valueOf(100.00);
        assertBigDecimalEquals(value.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.applyDiscount(value, -10), "100 after -10% off");
        assertBigDecimalEquals(value.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.applyDiscount(value, 110), "100 after 110% off");
    }

    @Test
    @DisplayName("calculateDiscountAmount should handle zero and null input values")
    void calculateDiscountAmount_shouldHandleZeroAndNullValue() {
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.calculateDiscountAmount(BigDecimal.ZERO, 10), "10% discount on 0");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.calculateDiscountAmount(null, 10), "10% discount on null");
    }

    @Test
    @DisplayName("calculateDiscountAmount should return zero for negative or >100 percentage")
    void calculateDiscountAmount_shouldHandleInvalidPercent() {
        BigDecimal originalValue = BigDecimal.valueOf(100.00);
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.calculateDiscountAmount(originalValue, -10), "Negative percentage discount");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.calculateDiscountAmount(originalValue, 110), "Percentage > 100 discount");
    }


    @Test
    @DisplayName("add should correctly add two BigDecimal values with scaling")
    void add_shouldAddCorrectly() {
        BigDecimal a = new BigDecimal("10.55");
        BigDecimal b = new BigDecimal("20.45");
        assertBigDecimalEquals(new BigDecimal("31.00"), BigDecimalUtil.add(a, b), "Adding 10.55 and 20.45");

        BigDecimal c = new BigDecimal("10.555");
        BigDecimal d = new BigDecimal("20.445");
        assertBigDecimalEquals(new BigDecimal("31.00"), BigDecimalUtil.add(c, d), "Adding 10.555 and 20.445 with rounding"); // 31.000 -> 31.00

        assertBigDecimalEquals(new BigDecimal("10.55"), BigDecimalUtil.add(a, BigDecimal.ZERO), "Adding with zero");
        assertBigDecimalEquals(new BigDecimal("10.55"), BigDecimalUtil.add(a, null), "Adding with null");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.add(null, null), "Adding nulls");
    }

    @Test
    @DisplayName("subtract should correctly subtract two BigDecimal values with scaling")
    void subtract_shouldSubtractCorrectly() {
        BigDecimal a = new BigDecimal("30.55");
        BigDecimal b = new BigDecimal("10.50");
        assertBigDecimalEquals(new BigDecimal("20.05"), BigDecimalUtil.subtract(a, b), "Subtracting 10.50 from 30.55");

        BigDecimal c = new BigDecimal("30.555");
        BigDecimal d = new BigDecimal("10.504");
        assertBigDecimalEquals(new BigDecimal("20.05"), BigDecimalUtil.subtract(c, d), "Subtracting with rounding"); // 20.051 -> 20.05

        assertBigDecimalEquals(new BigDecimal("30.55"), BigDecimalUtil.subtract(a, BigDecimal.ZERO), "Subtracting zero");
        assertBigDecimalEquals(new BigDecimal("-10.50"), BigDecimalUtil.subtract(BigDecimal.ZERO, b), "Subtracting from zero");
        assertBigDecimalEquals(new BigDecimal("30.55"), BigDecimalUtil.subtract(a, null), "Subtracting null");
        assertBigDecimalEquals(new BigDecimal("-10.50"), BigDecimalUtil.subtract(null, b), "Subtracting from null");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.subtract(null, null), "Subtracting nulls");
    }

    @Test
    @DisplayName("multiply should correctly multiply two BigDecimal values with scaling")
    void multiply_shouldMultiplyCorrectly() {
        BigDecimal a = new BigDecimal("10.50");
        BigDecimal b = new BigDecimal("2.00");
        assertBigDecimalEquals(new BigDecimal("21.00"), BigDecimalUtil.multiply(a, b), "Multiplying 10.50 by 2.00");

        BigDecimal c = new BigDecimal("10.555");
        BigDecimal d = new BigDecimal("2.00");
        assertBigDecimalEquals(new BigDecimal("21.11"), BigDecimalUtil.multiply(c, d), "Multiplying 10.555 by 2.00 with rounding"); // 21.110 -> 21.11

        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.multiply(a, BigDecimal.ZERO), "Multiplying by zero");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.multiply(a, null), "Multiplying by null");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.multiply(null, b), "Multiplying null");
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(BigDecimalUtil.SCALE, BigDecimalUtil.ROUNDING_MODE), BigDecimalUtil.multiply(null, null), "Multiplying nulls");
    }


    @Test
    @DisplayName("divide should correctly divide two BigDecimal values with scaling")
    void divide_shouldDivideCorrectly() {
        BigDecimal a = new BigDecimal("21.00");
        BigDecimal b = new BigDecimal("2.00");
        assertBigDecimalEquals(new BigDecimal("10.50"), BigDecimalUtil.divide(a, b), "Dividing 21.00 by 2.00");

        BigDecimal c = new BigDecimal("10.00");
        BigDecimal d = new BigDecimal("3.00");
        assertBigDecimalEquals(new BigDecimal("3.33"), BigDecimalUtil.divide(c, d), "Dividing 10.00 by 3.00 with rounding"); // 3.3333... -> 3.33

    }

    @Test
    @DisplayName("divide should throw ArithmeticException for division by zero")
    void divide_shouldThrowExceptionWhenDivideByZero() {
        BigDecimal a = BigDecimal.valueOf(10.00);
        BigDecimal b = BigDecimal.ZERO;
        assertThrows(ArithmeticException.class, () -> BigDecimalUtil.divide(a, b), "Should throw ArithmeticException for division by zero");
    }

    @Test
    @DisplayName("divide should throw ArithmeticException for null divisor")
    void divide_shouldThrowExceptionWhenDivideByNull() {
        BigDecimal a = BigDecimal.valueOf(10.00);
        assertThrows(ArithmeticException.class, () -> BigDecimalUtil.divide(a, null), "Should throw ArithmeticException for null divisor");
    }

    @Test
    @DisplayName("divide should throw ArithmeticException for null dividend and null divisor")
    void divide_shouldThrowExceptionWhenBothNull() {
        assertThrows(ArithmeticException.class, () -> BigDecimalUtil.divide(null, null), "Should throw ArithmeticException for null dividend and divisor");
    }

    @Test
    @DisplayName("divide should throw ArithmeticException for null dividend and zero divisor")
    void divide_shouldThrowExceptionWhenDividendNullDivisorZero() {
        assertThrows(ArithmeticException.class, () -> BigDecimalUtil.divide(null, BigDecimal.ZERO), "Should throw ArithmeticException for null dividend and zero divisor");
    }


    @Test
    @DisplayName("min should return the minimum of two BigDecimal values")
    void min_shouldReturnMinimum() {
        BigDecimal a = new BigDecimal("10.50");
        BigDecimal b = new BigDecimal("20.50");
        assertBigDecimalEquals(a, BigDecimalUtil.min(a, b), "Min of 10.50 and 20.50");
        assertBigDecimalEquals(a, BigDecimalUtil.min(b, a), "Min of 20.50 and 10.50");

        assertBigDecimalEquals(a, BigDecimalUtil.min(a, a), "Min of same values");

        assertBigDecimalEquals(a, BigDecimalUtil.min(a, null), "Min with null");
        assertBigDecimalEquals(b, BigDecimalUtil.min(null, b), "Min with null");
        assertNull(BigDecimalUtil.min(null, null), "Min of nulls"); // Note: depends on desired behavior, current impl returns null
    }

    @Test
    @DisplayName("max should return the maximum of two BigDecimal values")
    void max_shouldReturnMaximum() {
        BigDecimal a = new BigDecimal("10.50");
        BigDecimal b = new BigDecimal("20.50");
        assertBigDecimalEquals(b, BigDecimalUtil.max(a, b), "Max of 10.50 and 20.50");
        assertBigDecimalEquals(b, BigDecimalUtil.max(b, a), "Max of 20.50 and 10.50");

        assertBigDecimalEquals(a, BigDecimalUtil.max(a, a), "Max of same values");

        assertBigDecimalEquals(a, BigDecimalUtil.max(a, null), "Max with null");
        assertBigDecimalEquals(b, BigDecimalUtil.max(null, b), "Max with null");
        assertNull(BigDecimalUtil.max(null, null), "Max of nulls"); // Note: depends on desired behavior, current impl returns null
    }
}