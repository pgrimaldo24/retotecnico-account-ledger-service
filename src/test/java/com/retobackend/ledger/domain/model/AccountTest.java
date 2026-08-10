package com.retobackend.ledger.domain.model;

import com.retobackend.ledger.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account(AccountId.of("ACC-100"), Money.of(new BigDecimal("1000.00")));
    }

    @Test
    void shouldReserveFundsWhenBalanceIsSufficient() {
        account.reserve(Money.of(new BigDecimal("300.00")));

        assertEquals(new BigDecimal("700.00"), account.getAvailableBalance().getAmount());
        assertEquals(new BigDecimal("300.00"), account.getReservedBalance().getAmount());
    }

    @Test
    void shouldThrowWhenReservingMoreThanAvailable() {
        assertThrows(InsufficientFundsException.class,
                () -> account.reserve(Money.of(new BigDecimal("5000.00"))));


        assertEquals(new BigDecimal("1000.00"), account.getAvailableBalance().getAmount());
    }

    @Test
    void shouldConfirmDebitAfterReserve() {
        account.reserve(Money.of(new BigDecimal("300.00")));
        account.confirmDebit(Money.of(new BigDecimal("300.00")));

        assertEquals(new BigDecimal("700.00"), account.getAvailableBalance().getAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), account.getReservedBalance().getAmount().setScale(2));
        assertEquals(new BigDecimal("700.00"), account.getTotalBalance().getAmount());
    }

    @Test
    void shouldCreditTargetAccount() {
        account.credit(Money.of(new BigDecimal("200.00")));
        assertEquals(new BigDecimal("1200.00"), account.getAvailableBalance().getAmount());
    }

    @Test
    void shouldReleaseFundsOnCompensation() {
        account.reserve(Money.of(new BigDecimal("300.00")));
        account.release(Money.of(new BigDecimal("300.00")));

        assertEquals(new BigDecimal("1000.00"), account.getAvailableBalance().getAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), account.getReservedBalance().getAmount().setScale(2));
    }

    @Test
    void totalBalanceShouldRemainConstantAfterReserve() {
        Money totalBefore = account.getTotalBalance();
        account.reserve(Money.of(new BigDecimal("400.00")));
        Money totalAfter = account.getTotalBalance();

        assertEquals(totalBefore.getAmount(), totalAfter.getAmount());
    }

    @Test
    void shouldHandleConcurrentReservesWithoutRaceCondition() throws InterruptedException {
        int threads = 100;
        Money reserveAmount = Money.of(new BigDecimal("10.00"));
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    account.reserve(reserveAmount);
                } catch (InsufficientFundsException ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();


        assertEquals(new BigDecimal("0.00"), account.getAvailableBalance().getAmount().setScale(2));
        assertEquals(new BigDecimal("1000.00"), account.getReservedBalance().getAmount().setScale(2));
    }
}
