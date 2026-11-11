package com.fiap.workshop.billing.domain.model.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("should create money with 2 decimal places when using of(double)")
    void shouldCreateMoneyWith2DecimalPlacesWhenUsingOfDouble() {
        Money money = Money.of(10.5);
        assertEquals(new BigDecimal("10.50"), money.getAmount());
    }

    @Test
    @DisplayName("should return 0.00 when calling zero()")
    void shouldReturnZeroWhenCallingZero() {
        assertEquals(new BigDecimal("0.00"), Money.zero().getAmount());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when amount is null")
    void shouldThrowIllegalArgumentExceptionWhenAmountIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null));
    }

    @Test
    @DisplayName("should return sum when adding two Money values")
    void shouldReturnSumWhenAddingTwoMoneyValues() {
        Money result = Money.of(10.00).add(Money.of(5.50));
        assertEquals(Money.of(15.50), result);
    }

    @Test
    @DisplayName("should return multiplied amount when multiplying by quantity")
    void shouldReturnMultipliedAmountWhenMultiplyingByQuantity() {
        Money result = Money.of(25.00).multiply(3);
        assertEquals(Money.of(75.00), result);
    }

    @Test
    @DisplayName("should return true when amount is greater than other")
    void shouldReturnTrueWhenAmountIsGreaterThanOther() {
        assertTrue(Money.of(10.00).isGreaterThan(Money.of(5.00)));
    }

    @Test
    @DisplayName("should return false when amounts are equal")
    void shouldReturnFalseWhenAmountsAreEqual() {
        assertFalse(Money.of(10.00).isGreaterThan(Money.of(10.00)));
    }

    @Test
    @DisplayName("should be equal when amounts differ only in trailing zeros")
    void shouldBeEqualWhenAmountsDifferOnlyInTrailingZeros() {
        assertEquals(Money.of(new BigDecimal("10.0")), Money.of(new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("should have consistent hashCode with equals")
    void shouldHaveConsistentHashCodeWithEquals() {
        assertEquals(
                Money.of(10.00).hashCode(),
                Money.of(new BigDecimal("10.0")).hashCode());
    }

    @Test
    @DisplayName("should have consistent hashCode for two zero Money instances")
    void shouldHaveConsistentHashCodeForTwoZeroMoneyInstances() {
        assertEquals(Money.zero(), Money.zero());
        assertEquals(Money.zero().hashCode(), Money.zero().hashCode());
    }

    @Test
    @DisplayName("should return plain string when calling toString")
    void shouldReturnPlainStringWhenCallingToString() {
        assertEquals("10.50", Money.of(10.5).toString());
    }
}
