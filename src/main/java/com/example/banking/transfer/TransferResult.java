package com.example.banking.transfer;

import com.example.banking.transaction.AccountTransaction;

public record TransferResult(AccountTransaction debit, AccountTransaction credit) {
}
