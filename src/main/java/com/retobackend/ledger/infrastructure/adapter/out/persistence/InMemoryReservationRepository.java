package com.retobackend.ledger.infrastructure.adapter.out.persistence;

import com.retobackend.ledger.application.port.out.ReservationRepositoryPort;
import com.retobackend.ledger.domain.exception.ReservationNotFoundException;
import com.retobackend.ledger.domain.model.PendingReservation;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryReservationRepository implements ReservationRepositoryPort {

    private final Map<String, PendingReservation> storage = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> save(PendingReservation reservation) {
        storage.put(reservation.getTransactionId(), reservation);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<PendingReservation> findByTransactionId(String transactionId) {
        PendingReservation reservation = storage.get(transactionId);
        if (reservation == null) {
            return Uni.createFrom().failure(new ReservationNotFoundException(transactionId));
        }
        return Uni.createFrom().item(reservation);
    }

    @Override
    public Uni<Void> remove(String transactionId) {
        storage.remove(transactionId);
        return Uni.createFrom().voidItem();
    }
}