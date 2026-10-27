package com.example.banking.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(
        @NotNull(message = "sourceAccountId is required") UUID sourceAccountId,
        @NotNull(message = "destinationAccountId is required") UUID destinationAccountId,
        @NotNull(message = "amount is required")
                @DecimalMin(value = "0", inclusive = false, message = "amount must be greater than 0")
                @Digits(integer = 12, fraction = 2, message = "amount must have at most 12 integer and 2 decimal digits")
                BigDecimal amount,
        @NotBlank(message = "referenceId is required")
                @Size(max = 100, message = "referenceId must be at most 100 characters")
                String referenceId,
        @Size(max = 500, message = "description must be at most 500 characters") String description) {
}
