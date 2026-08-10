package com.retobackend.ledger.application.service;

import com.retobackend.ledger.application.port.in.CompensateTransactionUseCase;
import com.retobackend.ledger.application.port.in.ConfirmTransactionUseCase;
import com.retobackend.ledger.application.port.in.ReserveFundsUseCase;
import com.retobackend.ledger.application.port.out.AccountRepositoryPort;
import com.retobackend.ledger.application.port.out.ReservationRepositoryPort;
import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import com.retobackend.ledger.domain.model.PendingReservation;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class LedgerApplicationService implements
        ReserveFundsUseCase, ConfirmTransactionUseCase, CompensateTransactionUseCase {
    private static final double SIMULATED_FAILURE_RATE = 0.25;

    private final AccountRepositoryPort accountRepository;
    private final ReservationRepositoryPort reservationRepository;

    @Override
    public Uni<Void> reserve(String transactionId, AccountId sourceAccountId, AccountId targetAccountId, Money amount) {
        return accountRepository.findById(sourceAccountId)
                .invoke(account -> account.reserve(amount))
                .chain(() -> reservationRepository.save(
                        new PendingReservation(transactionId, sourceAccountId, targetAccountId, amount)))
                .invoke(() -> log.info("Reserva registrada para transacción {}", transactionId));
    }

    @Override
    public Uni<Void> confirm(String transactionId) {
        if (ThreadLocalRandom.current().nextDouble() < SIMULATED_FAILURE_RATE) {
            log.warn("Fallo simulado en ConfirmTransaction para transacción {}", transactionId);
            return Uni.createFrom().failure(
                    new RuntimeException("UNAVAILABLE: fallo simulado en ConfirmTransaction"));
        }
        return reservationRepository.findByTransactionId(transactionId)
                .chain(this::applyConfirmation);
    }

    private Uni<Void> applyConfirmation(PendingReservation reservation) {
        return accountRepository.findById(reservation.getSourceAccountId())
                .invoke(source -> source.confirmDebit(reservation.getAmount()))
                .chain(() -> accountRepository.findById(reservation.getTargetAccountId()))
                .invoke(target -> target.credit(reservation.getAmount()))
                .chain(() -> reservationRepository.remove(reservation.getTransactionId()))
                .invoke(() -> log.info("Transacción {} confirmada", reservation.getTransactionId()));
    }

    @Override
    public Uni<Void> compensate(String transactionId, String reason) {
        return reservationRepository.findByTransactionId(transactionId)
                .chain(reservation -> accountRepository.findById(reservation.getSourceAccountId())
                        .invoke(source -> source.release(reservation.getAmount()))
                        .chain(() -> reservationRepository.remove(transactionId)))
                .invoke(() -> log.info("Transacción {} compensada. Motivo: {}", transactionId, reason));
    }
}