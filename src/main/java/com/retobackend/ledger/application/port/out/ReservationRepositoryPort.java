package com.retobackend.ledger.application.port.out;

import com.retobackend.ledger.domain.model.PendingReservation;
import io.smallrye.mutiny.Uni;

public interface ReservationRepositoryPort {
    Uni<Void> save(PendingReservation reservation);
    Uni<PendingReservation> findByTransactionId(String transactionId);
    Uni<Void> remove(String transactionId);
}