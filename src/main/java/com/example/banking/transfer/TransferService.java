package com.example.banking.transfer;

import com.example.banking.account.Account;
import com.example.banking.account.AccountRepository;
import com.example.banking.common.AccountNotFoundException;
import com.example.banking.common.DuplicateTransactionException;
import com.example.banking.common.InsufficientFundsException;
import com.example.banking.common.InvalidRequestException;
import com.example.banking.transaction.AccountTransaction;
import com.example.banking.transaction.AccountTransactionRepository;
import com.example.banking.transaction.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository, AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResult transfer(
            UUID sourceId, UUID destinationId, BigDecimal amount, String referenceId, String description) {
        if (sourceId.equals(destinationId)) {
            throw new InvalidRequestException("sourceAccountId and destinationAccountId must be different");
        }

        List<Account> lockedAccounts = accountRepository.findAllByIdForUpdate(List.of(sourceId, destinationId));
        Map<UUID, Account> accounts = lockedAccounts.stream()
                .collect(Collectors.toMap(Account::getId, account -> account));
        Account source = accounts.get(sourceId);
        Account destination = accounts.get(destinationId);
        if (source == null) {
            throw new AccountNotFoundException(sourceId);
        }
        if (destination == null) {
            throw new AccountNotFoundException(destinationId);
        }
        if (transactionRepository.existsByAccount_IdAndReferenceId(sourceId, referenceId)
                || transactionRepository.existsByAccount_IdAndReferenceId(destinationId, referenceId)) {
            throw new DuplicateTransactionException(referenceId);
        }
        if (!source.debit(amount)) {
            throw new InsufficientFundsException(sourceId);
        }
        destination.credit(amount);
        accountRepository.saveAll(List.of(source, destination));

        AccountTransaction debit =
                new AccountTransaction(source, TransactionType.DEBIT, amount, description, referenceId);
        AccountTransaction credit =
                new AccountTransaction(destination, TransactionType.CREDIT, amount, description, referenceId);
        transactionRepository.saveAllAndFlush(List.of(debit, credit));
        return new TransferResult(debit, credit);
    }
}
