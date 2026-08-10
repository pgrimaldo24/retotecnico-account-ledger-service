package com.retobackend.ledger.application.port.in;

import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import io.smallrye.mutiny.Uni;

public interface ReserveFundsUseCase {
    Uni<Void> reserve(String transactionId, AccountId sourceAccount, AccountId targetAccount, Money amount);
}
