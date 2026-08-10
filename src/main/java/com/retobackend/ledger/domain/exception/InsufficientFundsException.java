package com.retobackend.ledger.domain.exception;

import com.retobackend.ledger.domain.model.AccountId;
import com.retobackend.ledger.domain.model.Money;
import lombok.Getter;

@Getter
public class InsufficientFundsException extends RuntimeException {
    private final AccountId accountId;
    private final Money requestedAmount;

    public InsufficientFundsException(AccountId accountId, Money requestedAmount) {
        super("Fondos insuficientes en la cuenta " + accountId.getValue());
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
    }
}
