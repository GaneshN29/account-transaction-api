package com.example.banking.account;

import com.example.banking.common.AccountNotFoundException;
import com.example.banking.common.DuplicateTransactionException;
import com.example.banking.common.InsufficientFundsException;
import com.example.banking.transaction.AccountTransaction;
import com.example.banking.transaction.AccountTransactionRepository;
import com.example.banking.transaction.TransactionType;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Account createAccount(String name) {
        return accountRepository.save(new Account(name.trim()));
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    public AccountTransaction createTransaction(
            UUID accountId, TransactionType type, BigDecimal amount, String description, String referenceId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (transactionRepository.existsByAccount_IdAndReferenceId(accountId, referenceId)) {
            throw new DuplicateTransactionException(referenceId);
        }
        applyBalanceChange(account, type, amount);
        AccountTransaction transaction =
                new AccountTransaction(account, type, amount, description, referenceId);
        accountRepository.save(account);
        return transactionRepository.saveAndFlush(transaction);
    }

    @Transactional(readOnly = true)
    public Page<AccountTransaction> getTransactions(
            UUID accountId, int page, int limit, TransactionType type) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        PageRequest pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        if (type != null) {
            return transactionRepository.findByAccount_IdAndType(accountId, type, pageable);
        }
        return transactionRepository.findByAccount_Id(accountId, pageable);
    }

    private void applyBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.CREDIT) {
            account.credit(amount);
        } else if (!account.debit(amount)) {
            throw new InsufficientFundsException(account.getId());
        }
    }
}
