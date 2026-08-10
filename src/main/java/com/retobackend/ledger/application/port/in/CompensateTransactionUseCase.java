package com.retobackend.ledger.application.port.in;

import io.smallrye.mutiny.Uni;

public interface CompensateTransactionUseCase {
    Uni<Void> compensate(String transactionId, String reason);
}