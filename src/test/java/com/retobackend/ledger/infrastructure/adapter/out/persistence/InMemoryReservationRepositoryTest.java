package com.retobackend.ledger.infrastructure.adapter.out.persistence;

import com.retobackend.ledger.domain.exception.ReservationNotFoundException;
import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import com.retobackend.ledger.domain.model.PendingReservation;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class InMemoryReservationRepositoryTest {

    @Inject
    InMemoryReservationRepository repository;

    @Test
    void shouldSaveAndFindReservationByTransactionId() {
        PendingReservation reservation = new PendingReservation(
                "tx-repo-001", AccountId.of("ACC-100"), AccountId.of("ACC-200"), Money.of(new BigDecimal("100.00")));

        repository.save(reservation).subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        UniAssertSubscriber<PendingReservation> subscriber = repository.findByTransactionId("tx-repo-001")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        PendingReservation found = subscriber.assertCompleted().getItem();
        assertEquals(reservation, found);
    }

    @Test
    void shouldFailWhenReservationNotFound() {
        UniAssertSubscriber<PendingReservation> subscriber = repository.findByTransactionId("tx-repo-missing")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ReservationNotFoundException.class);
    }

    @Test
    void shouldNoLongerFindReservationAfterRemove() {
        PendingReservation reservation = new PendingReservation(
                "tx-repo-002", AccountId.of("ACC-100"), AccountId.of("ACC-200"), Money.of(new BigDecimal("50.00")));
        repository.save(reservation).subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        repository.remove("tx-repo-002").subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        UniAssertSubscriber<PendingReservation> subscriber = repository.findByTransactionId("tx-repo-002")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ReservationNotFoundException.class);
    }
}
