package com.example.banking.common;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID id) {
        super("Insufficient balance for account: " + id);
    }
}
