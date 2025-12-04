package com.safipay.wallet.service;

import com.safipay.wallet.model.Transaction;
import com.safipay.wallet.model.Wallet;
import com.safipay.wallet.repository.TransactionRepository;
import com.safipay.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository repo;
    private final TransactionRepository txRepo;

    public Wallet createWallet(UUID userId) {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();

        return repo.save(wallet);
    }

    public Wallet credit(UUID walletId, BigDecimal amount) {
        Wallet wallet = repo.lockWalletForUpdate(walletId);
        wallet.setBalance(wallet.getBalance().add(amount));
        repo.save(wallet);

        saveTx(walletId, amount, "CREDIT");

        return wallet;
    }

    public Wallet debit(UUID walletId, BigDecimal amount) {
        Wallet wallet = repo.lockWalletForUpdate(walletId);
        if (wallet.getBalance().compareTo(amount) < 0)
            throw new RuntimeException("Insufficient balance");

        wallet.setBalance(wallet.getBalance().subtract(amount));
        repo.save(wallet);

        saveTx(walletId, amount, "DEBIT");

        return wallet;
    }

    @Transactional
    public void transfer(UUID from, UUID to, BigDecimal amount) {

        // Always lock in consistent order to avoid deadlocks
        Wallet source = repo.lockWalletForUpdate(from);
        Wallet destination = repo.lockWalletForUpdate(to);

        if (source.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        repo.save(source);
        repo.save(destination);
    }


    private void saveTx(UUID walletId, BigDecimal amount, String type) {
        Transaction tx = Transaction.builder()
                .walletId(walletId)
                .amount(amount)
                .type(type)
                .timestamp(System.currentTimeMillis())
                .build();

        txRepo.save(tx);
    }
}

