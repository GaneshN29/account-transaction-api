package com.example.banking.transaction;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    Page<AccountTransaction> findByAccount_Id(UUID accountId, Pageable pageable);

    Page<AccountTransaction> findByAccount_IdAndType(
            UUID accountId, TransactionType type, Pageable pageable);

    boolean existsByAccount_IdAndReferenceId(UUID accountId, String referenceId);
}
