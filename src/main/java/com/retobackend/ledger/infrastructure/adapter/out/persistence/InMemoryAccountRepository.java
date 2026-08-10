package com.retobackend.ledger.infrastructure.adapter.out.persistence;

import com.retobackend.ledger.application.port.out.AccountRepositoryPort;
import com.retobackend.ledger.domain.exception.AccountNotFoundException;
import com.retobackend.ledger.domain.model.Account;
import com.retobackend.ledger.domain.model.AccountId;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryAccountRepository implements AccountRepositoryPort {

    private final Map<String, Account> storage = new ConcurrentHashMap<>();

    public Uni<Void> save(Account account) {
        storage.put(account.getId().getValue(), account);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Account> findById(AccountId id) {
        Account account = storage.get(id.getValue());
        if (account == null) {
            return Uni.createFrom().failure(new AccountNotFoundException(id));
        }
        return Uni.createFrom().item(account);
    }
}