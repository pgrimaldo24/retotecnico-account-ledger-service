package com.retobackend.ledger.domain.model;

import lombok.NonNull;
import lombok.Value;

@Value
public class PendingReservation {
    @NonNull String transactionId;
    @NonNull AccountId sourceAccountId;
    @NonNull AccountId targetAccountId;
    @NonNull Money amount;
}
