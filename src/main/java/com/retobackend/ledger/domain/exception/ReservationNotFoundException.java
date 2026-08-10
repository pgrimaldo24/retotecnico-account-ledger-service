package com.retobackend.ledger.domain.exception;

import lombok.Getter;

@Getter
public class ReservationNotFoundException extends RuntimeException {
    private final String transactionId;

    public ReservationNotFoundException(String transactionId) {
        super("No existe una reserva pendiente para la transacción: " + transactionId);
        this.transactionId = transactionId;
    }
}
