package com.safipay.fraud.repository;

import com.safipay.fraud.model.FraudRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FraudRecordRepository extends JpaRepository<FraudRecord, UUID> {

    List<FraudRecord> findByWalletIdOrderByTimestampDesc(UUID walletId);

    @Query("""
        SELECT COUNT(f) FROM FraudRecord f
        WHERE f.walletId = :walletId
        AND f.timestamp >= :from
    """)
    long countRecentAttempts(UUID walletId, LocalDateTime from);
}
