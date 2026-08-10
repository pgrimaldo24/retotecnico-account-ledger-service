package com.retobackend.ledger.infrastructure.adapter.out.persistence;

import com.retobackend.ledger.domain.exception.AccountNotFoundException;
import com.retobackend.ledger.domain.model.Account;
import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class InMemoryAccountRepositoryTest {

    @Inject
    InMemoryAccountRepository repository;

    @Test
    void shouldFindAccountSeededAtStartup() {
        UniAssertSubscriber<Account> subscriber = repository.findById(AccountId.of("ACC-100"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Account account = subscriber.assertCompleted().getItem();
        assertEquals(AccountId.of("ACC-100"), account.getId());
    }

    @Test
    void shouldSaveAndRetrieveNewAccount() {
        Account account = new Account(AccountId.of("ACC-300"), Money.of(new BigDecimal("750.00")));

        repository.save(account).subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        UniAssertSubscriber<Account> subscriber = repository.findById(AccountId.of("ACC-300"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Account found = subscriber.assertCompleted().getItem();
        assertEquals(new BigDecimal("750.00"), found.getAvailableBalance().getAmount());
    }

    @Test
    void shouldFailWhenAccountNotFound() {
        UniAssertSubscriber<Account> subscriber = repository.findById(AccountId.of("ACC-404"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(AccountNotFoundException.class);
    }
}
