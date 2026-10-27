package com.example.banking.api;

import com.example.banking.account.AccountService;
import com.example.banking.transaction.AccountTransaction;
import com.example.banking.transaction.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an account",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Create account",
                            value = "{\"name\":\"Everyday account\"}"))),
            responses = @ApiResponse(
                    responseCode = "201",
                    description = "Account created",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Account created",
                            value = "{\"account_id\":\"c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e\",\"name\":\"Everyday account\",\"balance\":0.00,\"createdAt\":\"2026-10-27T09:00:00Z\"}"))))
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountService.createAccount(request.name()));
    }

    @GetMapping("/{accountId}")
    @Operation(
            summary = "Get an account and its current balance",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Account details",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Account details",
                            value = "{\"account_id\":\"c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e\",\"name\":\"Everyday account\",\"balance\":100.00,\"createdAt\":\"2026-10-27T09:00:00Z\"}"))))
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return AccountResponse.from(accountService.getAccount(accountId));
    }

    @PostMapping("/{accountId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an account credit or debit",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                                    name = "Credit",
                                    value = "{\"type\":\"credit\",\"amount\":100.00,\"description\":\"Initial deposit\",\"referenceId\":\"deposit-001\"}"))
                            ),
            responses = @ApiResponse(
                    responseCode = "201",
                    description = "Transaction created",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "Transaction created",
                            value = "{\"id\":\"a84eb439-6a36-4723-bb41-960b8f94935a\",\"accountId\":\"c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e\",\"type\":\"credit\",\"amount\":100.00,\"description\":\"Initial deposit\",\"referenceId\":\"deposit-001\",\"createdAt\":\"2026-10-27T09:00:00Z\"}"))))
    public TransactionResponse createTransaction(
            @Parameter(example = "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e") @PathVariable UUID accountId,
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionType type = TransactionType.parse(request.type());
        AccountTransaction transaction = accountService.createTransaction(
                accountId, type, request.amount(), request.description(), request.referenceId().trim());
        return TransactionResponse.from(transaction);
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(
            summary = "List transactions newest first",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Paginated transaction history",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Transaction page", value = """
                            {"currentPage":1,"pageSize":2,"totalRecords":3,"totalPages":2,"data":[{"id":"a84eb439-6a36-4723-bb41-960b8f94935a","accountId":"c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e","type":"credit","amount":100.00,"description":"Initial deposit","referenceId":"deposit-001","createdAt":"2026-10-27T09:00:00Z"},{"id":"b95fc540-7b57-4834-cc52-a517c006a46b","accountId":"c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e","type":"debit","amount":25.00,"description":"Purchase","referenceId":"purchase-001","createdAt":"2026-10-26T09:00:00Z"}]}
                            """))))
    public PaginationResponse<TransactionResponse> getTransactions(
            @Parameter(example = "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e") @PathVariable UUID accountId,
            @Parameter(
                            description = "Optional transaction type filter",
                            example = "credit",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    allowableValues = {"credit", "debit"}))
                    @RequestParam(required = false)
                    String type,
            @Parameter(example = "1", description = "1-based page number")
                    @RequestParam(defaultValue = "1")
                    @Min(value = 1, message = "page must be at least 1")
                    int page,
            @Parameter(example = "20", description = "Page size, from 1 to 100")
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "limit must be at least 1")
                    @Max(value = 100, message = "limit must be at most 100")
                    int limit) {
        TransactionType transactionType = type == null ? null : TransactionType.parse(type);
        Page<AccountTransaction> result =
                accountService.getTransactions(accountId, page, limit, transactionType);
        return new PaginationResponse<>(
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(TransactionResponse::from).toList());
    }
}
