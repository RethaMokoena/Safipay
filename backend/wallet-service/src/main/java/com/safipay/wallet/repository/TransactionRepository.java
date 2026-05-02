package com.safipay.wallet.repository;
import com.safipay.wallet.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(String walletId, Pageable pageable);
    List<Transaction> findByReferenceId(String referenceId);
}
