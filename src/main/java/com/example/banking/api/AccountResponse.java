package com.example.banking.api;

import com.example.banking.account.Account;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        @JsonProperty("account_id") UUID accountId, String name, BigDecimal balance, Instant createdAt) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getBalance(), account.getCreatedAt());
    }
}
