package com.retobackend.ledger.domain.model;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class AccountId {
    @NonNull String value;
}