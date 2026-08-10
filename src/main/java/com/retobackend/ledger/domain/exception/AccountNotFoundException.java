package com.retobackend.ledger.domain.exception;

import com.retobackend.ledger.domain.model.AccountId;
import lombok.Getter;

@Getter
public class AccountNotFoundException extends RuntimeException {
    private final AccountId accountId;

    public AccountNotFoundException(AccountId accountId) {
        super("Cuenta no encontrada: " + accountId.getValue());
        this.accountId = accountId;
    }
}