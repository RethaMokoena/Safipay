package com.safipay.ledger.repository;

import com.safipay.ledger.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, String> {
    Optional<LedgerAccount> findByOwnerId(String ownerId);
    boolean existsByOwnerId(String ownerId);
}
