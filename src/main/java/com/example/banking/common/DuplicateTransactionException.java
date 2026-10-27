package com.example.banking.common;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String referenceId) {
        super("A transaction with this referenceId already exists for the account: " + referenceId);
    }
}
