# API Reference

Base URL: `http://localhost:8080`

Interactive documentation: [Swagger UI](http://localhost:8080/swagger-ui/index.html)  
OpenAPI specification: [JSON](http://localhost:8080/v3/api-docs)

## Conventions

- Request and response bodies use JSON.
- Account and transaction identifiers are UUIDs.
- Amounts are decimal values with up to two fractional digits.
- Timestamps are ISO 8601 UTC instants.
- Transaction types are `credit` and `debit` in API requests and responses.
- Transaction history is ordered by creation time, newest first. Transaction ID is used as a tie-breaker.

## Accounts

### Create an account

`POST /api/accounts`

The account name is required and must contain non-whitespace characters. New accounts start with a zero balance.

Request:

```json
{
  "name": "Everyday account"
}
```

Response — `201 Created`:

```json
{
  "account_id": "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e",
  "name": "Everyday account",
  "balance": 0.00,
  "createdAt": "2026-10-27T09:00:00Z"
}
```

### Get an account and its balance

`GET /api/accounts/{accountId}`

Response — `200 OK`:

```json
{
  "account_id": "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e",
  "name": "Everyday account",
  "balance": 100.00,
  "createdAt": "2026-10-27T09:00:00Z"
}
```

Returns `404 Not Found` if the account does not exist.

## Transactions

### Create a credit or debit

`POST /api/accounts/{accountId}/transactions`

`type` must be `credit` or `debit`. `amount` must be greater than zero and have no more than two fractional digits. `referenceId` is required and unique within the account. `description` is optional.

Request:

```json
{
  "type": "credit",
  "amount": 100.00,
  "description": "Initial deposit",
  "referenceId": "deposit-001"
}
```

Response — `201 Created`:

```json
{
  "id": "a84eb439-6a36-4723-bb41-960b8f94935a",
  "accountId": "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e",
  "type": "credit",
  "amount": 100.00,
  "description": "Initial deposit",
  "referenceId": "deposit-001",
  "createdAt": "2026-10-27T09:00:00Z"
}
```

A debit is rejected if the account has insufficient funds. A rejected request does not create a transaction or change the balance.

### List transaction history

`GET /api/accounts/{accountId}/transactions`

Query parameters:

| Parameter | Required | Default | Description |
| --- | --- | --- | --- |
| `page` | No | `1` | 1-based page number; must be at least `1`. |
| `limit` | No | `20` | Page size; must be between `1` and `100`. |
| `type` | No | — | Filter by `credit` or `debit`. Omit to include both. |

Example request:

```text
GET /api/accounts/c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e/transactions?page=1&limit=20&type=credit
```

Response — `200 OK`:

```json
{
  "currentPage": 1,
  "pageSize": 20,
  "totalRecords": 1,
  "totalPages": 1,
  "data": [
    {
      "id": "a84eb439-6a36-4723-bb41-960b8f94935a",
      "accountId": "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e",
      "type": "credit",
      "amount": 100.00,
      "description": "Initial deposit",
      "referenceId": "deposit-001",
      "createdAt": "2026-10-27T09:00:00Z"
    }
  ]
}
```

An account with no matching transactions returns an empty `data` array. The account must exist; otherwise, the endpoint returns `404 Not Found`.

## Transfers

### Transfer funds between accounts

`POST /api/transfers`

Both accounts must exist, must be different, and the source account must have sufficient funds. The reference ID must not already exist on either account. The debit and credit entries, and both balance changes, are committed together.

Request:

```json
{
  "sourceAccountId": "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e",
  "destinationAccountId": "4d2011b9-c65f-4bb0-9fb5-01d4515aeef5",
  "amount": 30.00,
  "referenceId": "transfer-001",
  "description": "Shared expenses"
}
```

Response — `201 Created`:

```json
{
  "debit": {
    "id": "a84eb439-6a36-4723-bb41-960b8f94935a",
    "accountId": "c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e",
    "type": "debit",
    "amount": 30.00,
    "description": "Shared expenses",
    "referenceId": "transfer-001",
    "createdAt": "2026-10-27T09:00:00Z"
  },
  "credit": {
    "id": "b95fc540-7b57-4834-cc52-a517c006a46b",
    "accountId": "4d2011b9-c65f-4bb0-9fb5-01d4515aeef5",
    "type": "credit",
    "amount": 30.00,
    "description": "Shared expenses",
    "referenceId": "transfer-001",
    "createdAt": "2026-10-27T09:00:00Z"
  }
}
```

## Errors

Errors use the same response structure:

```json
{
  "timestamp": "2026-10-27T09:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient balance for account: c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e",
  "path": "/api/accounts/c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e/transactions",
  "fieldErrors": {}
}
```

For validation errors, `fieldErrors` contains the fields that failed validation:

```json
{
  "timestamp": "2026-10-27T09:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/accounts",
  "fieldErrors": {
    "name": "name is required"
  }
}
```

| Status | Meaning |
| --- | --- |
| `400 Bad Request` | Invalid request body, field value, UUID, transaction type, or pagination parameter. |
| `404 Not Found` | The requested account does not exist. |
| `409 Conflict` | Insufficient balance or a duplicate reference ID. |

## Data integrity

- A balance update and its transaction record are written in one database transaction.
- Account rows are locked during balance changes to serialize concurrent debits and credits.
- A unique database constraint enforces reference ID uniqueness per account.
- Transactions have no update or delete API. A PostgreSQL trigger also rejects updates and deletes to transaction rows.
- Transfers lock both accounts in UUID order and persist both ledger entries in the same database transaction.

## Assumptions

- The API handles one unspecified currency; currency conversion is outside the scope of this assessment.
- Amounts use two decimal places. The database stores them as `NUMERIC(14, 2)`.
- Accounts start with a zero balance. Credits are used to add funds.
- Reference IDs are unique per account, not globally. A transfer uses the same reference on its source and destination accounts.
- Transaction history uses 1-based pages and caps page size at 100.
