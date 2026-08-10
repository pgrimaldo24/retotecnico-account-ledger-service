package com.retobackend.ledger.domain.model;

import com.retobackend.ledger.domain.exception.InsufficientFundsException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
public class Account {

    private final AccountId id;
    private final AtomicReference<Money> availableBalance;
    private final AtomicReference<Money> reservedBalance;

    public Account(AccountId id, Money initialBalance) {
        this.id = id;
        this.availableBalance = new AtomicReference<>(initialBalance);
        this.reservedBalance = new AtomicReference<>(Money.of(BigDecimal.ZERO));
    }

    public void reserve(Money amount) {
        Money current;
        Money updated;
        do {
            current = availableBalance.get();
            if (!current.isGreaterOrEqualTo(amount)) {
                log.warn("Fondos insuficientes en cuenta {}: disponible={}, solicitado={}",
                        id.getValue(), current.getAmount(), amount.getAmount());
                throw new InsufficientFundsException(id, amount);
            }
            updated = current.subtract(amount);
        } while (!availableBalance.compareAndSet(current, updated));

        reservedBalance.updateAndGet(r -> r.add(amount));
        log.info("Reserva exitosa en cuenta {}: monto={}", id.getValue(), amount.getAmount());
    }

    public void confirmDebit(Money amount) {
        reservedBalance.updateAndGet(r -> r.subtract(amount));
        log.info("Débito confirmado en cuenta {}: monto={}", id.getValue(), amount.getAmount());
    }

    public void credit(Money amount) {
        availableBalance.updateAndGet(a -> a.add(amount));
        log.info("Crédito aplicado en cuenta {}: monto={}", id.getValue(), amount.getAmount());
    }

    public void release(Money amount) {
        reservedBalance.updateAndGet(r -> r.subtract(amount));
        availableBalance.updateAndGet(a -> a.add(amount));
        log.info("Fondos liberados (compensación) en cuenta {}: monto={}", id.getValue(), amount.getAmount());
    }

    public AccountId getId() {
        return id;
    }

    public Money getAvailableBalance() {
        return availableBalance.get();
    }

    public Money getReservedBalance() {
        return reservedBalance.get();
    }

    public Money getTotalBalance() {
        return availableBalance.get().add(reservedBalance.get());
    }
}