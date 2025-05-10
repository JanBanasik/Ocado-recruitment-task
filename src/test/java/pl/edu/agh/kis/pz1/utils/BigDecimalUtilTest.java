package pl.edu.agh.kis.pz1.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BigDecimalUtilTest {

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {

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
    @DisplayName("setScale should return zero for null input")
    void setScale_shouldReturnNullForNull() {
        assertEquals(BigDecimal.ZERO, BigDecimalUtil.setScale(null));
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
        BigDecimal expected10PercentOff = BigDecimal.valueOf(180.00);
        assertBigDecimalEquals(expected10PercentOff, BigDecimalUtil.applyDiscount(value, 10), "200 after 10% off");

        BigDecimal expected0PercentOff = BigDecimal.valueOf(200.00);
        assertBigDecimalEquals(expected0PercentOff, BigDecimalUtil.applyDiscount(value, 0), "200 after 0% off");

        BigDecimal expected100PercentOff = BigDecimal.valueOf(0.00);
        assertBigDecimalEquals(expected100PercentOff, BigDecimalUtil.applyDiscount(value, 100), "200 after 100% off");

        BigDecimal valueWithDecimals = new BigDecimal("150.50");
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
}