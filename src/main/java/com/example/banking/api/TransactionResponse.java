package com.example.banking.api;

import com.example.banking.transaction.AccountTransaction;
import com.example.banking.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        String description,
        String referenceId,
        Instant createdAt) {
    public static TransactionResponse from(AccountTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getReferenceId(),
                transaction.getCreatedAt());
    }
}
