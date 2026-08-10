package com.retobackend.ledger.application.service;

import com.retobackend.ledger.application.port.out.AccountRepositoryPort;
import com.retobackend.ledger.application.port.out.ReservationRepositoryPort;
import com.retobackend.ledger.domain.exception.InsufficientFundsException;
import com.retobackend.ledger.domain.exception.ReservationNotFoundException;
import com.retobackend.ledger.domain.model.Account;
import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import com.retobackend.ledger.domain.model.PendingReservation;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@QuarkusTest
class LedgerApplicationServiceTest {

    @InjectMock
    AccountRepositoryPort accountRepository;

    @InjectMock
    ReservationRepositoryPort reservationRepository;

    @Inject
    LedgerApplicationService service;

    Account sourceAccount;
    Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = new Account(AccountId.of("ACC-100"), Money.of(new BigDecimal("1000.00")));
        targetAccount = new Account(AccountId.of("ACC-200"), Money.of(new BigDecimal("500.00")));
    }

    @Test
    void shouldReserveFundsSuccessfully() {
        when(accountRepository.findById(AccountId.of("ACC-100")))
                .thenReturn(Uni.createFrom().item(sourceAccount));
        when(reservationRepository.save(any()))
                .thenReturn(Uni.createFrom().voidItem());

        UniAssertSubscriber<Void> subscriber = service.reserve(
                        "tx-001",
                        AccountId.of("ACC-100"),
                        AccountId.of("ACC-200"),
                        Money.of(new BigDecimal("200.00")))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        assertEquals(new BigDecimal("800.00"), sourceAccount.getAvailableBalance().getAmount());
        verify(reservationRepository, times(1)).save(any());
    }

    @Test
    void shouldFailWhenInsufficientFunds() {
        when(accountRepository.findById(AccountId.of("ACC-100")))
                .thenReturn(Uni.createFrom().item(sourceAccount));

        UniAssertSubscriber<Void> subscriber = service.reserve(
                        "tx-002",
                        AccountId.of("ACC-100"),
                        AccountId.of("ACC-200"),
                        Money.of(new BigDecimal("5000.00")))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InsufficientFundsException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldConfirmTransactionAndMoveFundsToTarget() {
        sourceAccount.reserve(Money.of(new BigDecimal("200.00")));
        PendingReservation reservation = new PendingReservation(
                "tx-003", AccountId.of("ACC-100"), AccountId.of("ACC-200"), Money.of(new BigDecimal("200.00")));

        when(reservationRepository.findByTransactionId("tx-003"))
                .thenReturn(Uni.createFrom().item(reservation));
        when(accountRepository.findById(AccountId.of("ACC-100")))
                .thenReturn(Uni.createFrom().item(sourceAccount));
        when(accountRepository.findById(AccountId.of("ACC-200")))
                .thenReturn(Uni.createFrom().item(targetAccount));
        when(reservationRepository.remove("tx-003"))
                .thenReturn(Uni.createFrom().voidItem());

        UniAssertSubscriber<Void> subscriber = confirmSkippingSimulatedFailures("tx-003");

        subscriber.assertCompleted();
        assertEquals(BigDecimal.ZERO.setScale(2), sourceAccount.getReservedBalance().getAmount().setScale(2));
        assertEquals(new BigDecimal("700.00"), targetAccount.getAvailableBalance().getAmount());
        verify(reservationRepository, times(1)).remove("tx-003");
    }

    @Test
    void shouldFailConfirmWhenReservationNotFound() {
        when(reservationRepository.findByTransactionId("tx-missing"))
                .thenReturn(Uni.createFrom().failure(new ReservationNotFoundException("tx-missing")));

        UniAssertSubscriber<Void> subscriber = confirmSkippingSimulatedFailures("tx-missing");

        subscriber.assertFailedWith(ReservationNotFoundException.class);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void shouldCompensateTransactionAndReleaseFunds() {
        sourceAccount.reserve(Money.of(new BigDecimal("150.00")));
        PendingReservation reservation = new PendingReservation(
                "tx-004", AccountId.of("ACC-100"), AccountId.of("ACC-200"), Money.of(new BigDecimal("150.00")));

        when(reservationRepository.findByTransactionId("tx-004"))
                .thenReturn(Uni.createFrom().item(reservation));
        when(accountRepository.findById(AccountId.of("ACC-100")))
                .thenReturn(Uni.createFrom().item(sourceAccount));
        when(reservationRepository.remove("tx-004"))
                .thenReturn(Uni.createFrom().voidItem());

        UniAssertSubscriber<Void> subscriber = service.compensate("tx-004", "timeout aguas arriba")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        assertEquals(new BigDecimal("1000.00"), sourceAccount.getAvailableBalance().getAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), sourceAccount.getReservedBalance().getAmount().setScale(2));
        verify(reservationRepository, times(1)).remove("tx-004");
    }

    @Test
    void shouldFailCompensateWhenReservationNotFound() {
        when(reservationRepository.findByTransactionId("tx-missing"))
                .thenReturn(Uni.createFrom().failure(new ReservationNotFoundException("tx-missing")));

        UniAssertSubscriber<Void> subscriber = service.compensate("tx-missing", "motivo de prueba")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ReservationNotFoundException.class);
        verify(accountRepository, never()).findById(any());
    }

    /**
     * confirm() intercala un fallo simulado (25%, ver SIMULATED_FAILURE_RATE) para
     * ejercitar las políticas de compensación del orquestador. Reintenta hasta obtener
     * un resultado real (éxito o fallo de negocio) e ignora ese ruido aleatorio.
     */
    private UniAssertSubscriber<Void> confirmSkippingSimulatedFailures(String transactionId) {
        for (int attempt = 0; attempt < 200; attempt++) {
            UniAssertSubscriber<Void> subscriber = service.confirm(transactionId)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());
            Throwable failure = subscriber.getFailure();
            boolean simulated = failure instanceof RuntimeException
                    && failure.getMessage() != null
                    && failure.getMessage().contains("fallo simulado");
            if (!simulated) {
                return subscriber;
            }
        }
        throw new IllegalStateException("No se pudo evitar el fallo simulado tras múltiples intentos");
    }
}
