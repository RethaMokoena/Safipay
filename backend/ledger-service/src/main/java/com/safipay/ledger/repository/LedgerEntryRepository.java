package com.safipay.ledger.repository;

import com.safipay.ledger.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {
    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);
    List<LedgerEntry> findByTransactionRef(String transactionRef);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM LedgerEntry e WHERE e.account.id = :accountId AND e.entryType = 'CREDIT'")
    BigDecimal sumCredits(String accountId);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM LedgerEntry e WHERE e.account.id = :accountId AND e.entryType = 'DEBIT'")
    BigDecimal sumDebits(String accountId);

    @Query("SELECT e FROM LedgerEntry e WHERE e.account.id = :accountId AND e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC")
    List<LedgerEntry> findByAccountAndDateRange(String accountId, LocalDateTime from, LocalDateTime to);
}
