package com.retobackend.ledger.application.port.in;

import io.smallrye.mutiny.Uni;

public interface ConfirmTransactionUseCase {
    Uni<Void> confirm(String transactionId);
}
