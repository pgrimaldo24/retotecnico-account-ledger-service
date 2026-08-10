package com.retobackend.ledger.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithValidAmount() {
        Money money = Money.of(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), money.getAmount());
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(new BigDecimal("-10.00")));
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(null));
    }

    @Test
    void shouldAddTwoAmountsCorrectly() {
        Money a = Money.of(new BigDecimal("100.00"));
        Money b = Money.of(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), a.add(b).getAmount());
    }

    @Test
    void shouldSubtractCorrectly() {
        Money a = Money.of(new BigDecimal("100.00"));
        Money b = Money.of(new BigDecimal("30.00"));
        assertEquals(new BigDecimal("70.00"), a.subtract(b).getAmount());
    }

    @Test
    void shouldRejectSubtractionResultingInNegative() {
        Money a = Money.of(new BigDecimal("50.00"));
        Money b = Money.of(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
    }

    @Test
    void shouldCompareAmountsCorrectly() {
        Money a = Money.of(new BigDecimal("100.00"));
        Money b = Money.of(new BigDecimal("100.00"));
        Money c = Money.of(new BigDecimal("50.00"));

        assertTrue(a.isGreaterOrEqualTo(b));
        assertTrue(a.isGreaterOrEqualTo(c));
        assertFalse(c.isGreaterOrEqualTo(a));
    }

    @Test
    void shouldBeEqualByValueNotReference() {
        Money a = Money.of(new BigDecimal("100.00"));
        Money b = Money.of(new BigDecimal("100.00"));
        assertEquals(a, b);
    }
}