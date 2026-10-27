package com.example.banking.api;

import com.example.banking.transfer.TransferService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/api/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Transfer funds between accounts atomically",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Transfer funds",
                            value = "{\"sourceAccountId\":\"c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e\",\"destinationAccountId\":\"4d2011b9-c65f-4bb0-9fb5-01d4515aeef5\",\"amount\":30.00,\"referenceId\":\"transfer-001\",\"description\":\"Shared expenses\"}"))),
            responses = @ApiResponse(
                    responseCode = "201",
                    description = "Transfer completed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Transfer completed",
                            value = "{\"debit\":{\"id\":\"a84eb439-6a36-4723-bb41-960b8f94935a\",\"accountId\":\"c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e\",\"type\":\"debit\",\"amount\":30.00,\"description\":\"Shared expenses\",\"referenceId\":\"transfer-001\",\"createdAt\":\"2026-10-27T09:00:00Z\"},\"credit\":{\"id\":\"b95fc540-7b57-4834-cc52-a517c006a46b\",\"accountId\":\"4d2011b9-c65f-4bb0-9fb5-01d4515aeef5\",\"type\":\"credit\",\"amount\":30.00,\"description\":\"Shared expenses\",\"referenceId\":\"transfer-001\",\"createdAt\":\"2026-10-27T09:00:00Z\"}}"))))
    public TransferResponse transfer(@Valid @RequestBody CreateTransferRequest request) {
        return TransferResponse.from(transferService.transfer(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.referenceId().trim(),
                request.description()));
    }
}
