package com.retobackend.ledger.infrastructure.config;

import com.retobackend.ledger.domain.model.Account;
import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import com.retobackend.ledger.infrastructure.adapter.out.persistence.InMemoryAccountRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class AccountCatalogInitializer {

    private final InMemoryAccountRepository accountRepository;

    void onStart(@Observes StartupEvent event) {
        accountRepository.save(new Account(AccountId.of("ACC-100"), Money.of(new BigDecimal("1000.00"))));
        accountRepository.save(new Account(AccountId.of("ACC-200"), Money.of(new BigDecimal("500.00"))));
        log.info("Catálogo de cuentas inicializado: ACC-100 ($1000.00), ACC-200 ($500.00)");
    }
}