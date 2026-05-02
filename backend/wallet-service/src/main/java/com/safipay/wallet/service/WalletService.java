package com.safipay.wallet.service;

import com.safipay.wallet.dto.request.*;
import com.safipay.wallet.dto.response.*;
import com.safipay.wallet.exception.WalletException;
import com.safipay.wallet.model.*;
import com.safipay.wallet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j @Transactional
public class WalletService {

    private final WalletRepository walletRepo;
    private final TransactionRepository txRepo;

    public WalletResponse createWallet(String userId) {
        if (walletRepo.existsByUserId(userId))
            throw new WalletException("Wallet already exists for user: " + userId);
        Wallet wallet = walletRepo.save(Wallet.builder().userId(userId).build());
        log.info("Created wallet {} for user {}", wallet.getId(), userId);
        return toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(String userId) {
        return toWalletResponse(getWalletByUser(userId));
    }

    public WalletResponse topUp(String userId, TopUpRequest req) {
        Wallet wallet = getWalletByUser(userId);
        BigDecimal before = wallet.getBalance();
        wallet.setBalance(before.add(req.getAmount()));
        wallet = walletRepo.save(wallet);

        saveTransaction(wallet, req.getAmount(), before, wallet.getBalance(),
            Transaction.TransactionType.CREDIT, Transaction.TransactionStatus.COMPLETED,
            req.getReferenceId(), "Top up");

        return toWalletResponse(wallet);
    }

    public WalletResponse transfer(String senderUserId, TransferRequest req) {
        if (senderUserId.equals(req.getRecipientUserId()))
            throw new WalletException("Cannot transfer to yourself");

        Wallet sender = getWalletByUser(senderUserId);
        Wallet recipient = getWalletByUser(req.getRecipientUserId());

        if (sender.getAvailableBalance().compareTo(req.getAmount()) < 0)
            throw new WalletException("Insufficient balance");

        BigDecimal senderBefore = sender.getBalance();
        BigDecimal recipientBefore = recipient.getBalance();

        sender.setBalance(senderBefore.subtract(req.getAmount()));
        recipient.setBalance(recipientBefore.add(req.getAmount()));

        walletRepo.save(sender);
        walletRepo.save(recipient);

        String desc = req.getDescription() != null ? req.getDescription() : "Transfer";
        saveTransaction(sender, req.getAmount(), senderBefore, sender.getBalance(),
            Transaction.TransactionType.DEBIT, Transaction.TransactionStatus.COMPLETED,
            null, desc, req.getRecipientUserId());
        saveTransaction(recipient, req.getAmount(), recipientBefore, recipient.getBalance(),
            Transaction.TransactionType.CREDIT, Transaction.TransactionStatus.COMPLETED,
            null, desc, senderUserId);

        log.info("Transfer R{} from {} to {}", req.getAmount(), senderUserId, req.getRecipientUserId());
        return toWalletResponse(sender);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(String userId, int page, int size) {
        Wallet wallet = getWalletByUser(userId);
        return txRepo.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(page, size))
            .stream().map(this::toTxResponse).collect(Collectors.toList());
    }

    // Internal: credit/debit directly (used by payment-service, stokvel-service)
    public void internalCredit(String userId, BigDecimal amount, String referenceId, String description) {
        Wallet wallet = getWalletByUser(userId);
        BigDecimal before = wallet.getBalance();
        wallet.setBalance(before.add(amount));
        walletRepo.save(wallet);
        saveTransaction(wallet, amount, before, wallet.getBalance(),
            Transaction.TransactionType.CREDIT, Transaction.TransactionStatus.COMPLETED, referenceId, description);
    }

    public void internalDebit(String userId, BigDecimal amount, String referenceId, String description) {
        Wallet wallet = getWalletByUser(userId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0)
            throw new WalletException("Insufficient balance for: " + referenceId);
        BigDecimal before = wallet.getBalance();
        wallet.setBalance(before.subtract(amount));
        walletRepo.save(wallet);
        saveTransaction(wallet, amount, before, wallet.getBalance(),
            Transaction.TransactionType.DEBIT, Transaction.TransactionStatus.COMPLETED, referenceId, description);
    }

    private Wallet getWalletByUser(String userId) {
        return walletRepo.findByUserId(userId)
            .orElseThrow(() -> new WalletException("Wallet not found for user: " + userId));
    }

    private void saveTransaction(Wallet w, BigDecimal amount, BigDecimal before, BigDecimal after,
            Transaction.TransactionType type, Transaction.TransactionStatus status,
            String refId, String desc) {
        saveTransaction(w, amount, before, after, type, status, refId, desc, null);
    }

    private void saveTransaction(Wallet w, BigDecimal amount, BigDecimal before, BigDecimal after,
            Transaction.TransactionType type, Transaction.TransactionStatus status,
            String refId, String desc, String counterparty) {
        txRepo.save(Transaction.builder()
            .wallet(w).amount(amount).balanceBefore(before).balanceAfter(after)
            .type(type).status(status).referenceId(refId).description(desc)
            .counterpartyUserId(counterparty).build());
    }

    private WalletResponse toWalletResponse(Wallet w) {
        return WalletResponse.builder()
            .id(w.getId()).userId(w.getUserId()).balance(w.getBalance())
            .lockedBalance(w.getLockedBalance()).availableBalance(w.getAvailableBalance())
            .currency(w.getCurrency()).status(w.getStatus()).createdAt(w.getCreatedAt()).build();
    }

    private TransactionResponse toTxResponse(Transaction t) {
        return TransactionResponse.builder()
            .id(t.getId()).walletId(t.getWallet().getId()).amount(t.getAmount())
            .balanceBefore(t.getBalanceBefore()).balanceAfter(t.getBalanceAfter())
            .type(t.getType()).status(t.getStatus()).referenceId(t.getReferenceId())
            .description(t.getDescription()).counterpartyUserId(t.getCounterpartyUserId())
            .createdAt(t.getCreatedAt()).build();
    }
}
