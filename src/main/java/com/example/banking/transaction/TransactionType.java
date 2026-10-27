package com.example.banking.transaction;

import com.example.banking.common.InvalidRequestException;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum TransactionType {
    CREDIT,
    DEBIT;

    public static TransactionType parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidRequestException("type must be either credit or debit");
        }
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
