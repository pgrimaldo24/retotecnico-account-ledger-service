package com.retobackend.ledger.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@EqualsAndHashCode
public class Money {
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo o nulo");
        }
        this.amount = amount;
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        if (this.amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException("Resultado no puede ser negativo");
        }
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean isGreaterOrEqualTo(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }
}
