package com.safipay.wallet.repository;
import com.safipay.wallet.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
public interface TransactionRepository  extends JpaRepository<Transaction, UUID> {
    public default Optional<Transaction> findById(UUID uuid) {
          return Optional.empty();
    }
}
