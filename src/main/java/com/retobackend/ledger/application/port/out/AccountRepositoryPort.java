package com.retobackend.ledger.application.port.out;

import com.retobackend.ledger.domain.model.Account;
import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.exception.AccountNotFoundException;
import io.smallrye.mutiny.Uni;

public interface AccountRepositoryPort {
    Uni<Account> findById(AccountId id) throws AccountNotFoundException;
}