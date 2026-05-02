package com.safipay.ledger.repository;

import com.safipay.ledger.model.LedgerTransaction;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, String> {
    Optional<LedgerTransaction> findByExternalRef(String externalRef);
    Page<LedgerTransaction> findByDebitAccountIdOrCreditAccountIdOrderByCreatedAtDesc(
            String debitAccountId, String creditAccountId, Pageable pageable);
    boolean existsByExternalRef(String externalRef);
}
