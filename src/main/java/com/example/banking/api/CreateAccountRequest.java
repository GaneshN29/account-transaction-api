package com.example.banking.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "name is required") @Size(max = 120, message = "name must be at most 120 characters")
        String name) {
}
