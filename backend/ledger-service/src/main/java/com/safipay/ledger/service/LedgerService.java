package com.safipay.ledger.service;

import com.safipay.ledger.dto.request.CreateAccountRequest;
import com.safipay.ledger.dto.request.PostTransactionRequest;
import com.safipay.ledger.dto.response.*;
import com.safipay.ledger.exception.LedgerException;
import com.safipay.ledger.model.*;
import com.safipay.ledger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LedgerService {

    private final LedgerAccountRepository accountRepo;
    private final LedgerEntryRepository entryRepo;
    private final LedgerTransactionRepository txRepo;

    // ── Accounts ──────────────────────────────────────────────────

    public LedgerAccountResponse createAccount(CreateAccountRequest req) {
        if (accountRepo.existsByOwnerId(req.getOwnerId())) {
            throw new LedgerException("Ledger account already exists for owner: " + req.getOwnerId());
        }
        LedgerAccount account = accountRepo.save(LedgerAccount.builder()
                .ownerId(req.getOwnerId())
                .type(req.getType())
                .description(req.getDescription())
                .build());
        log.info("Created ledger account {} for owner {}", account.getId(), req.getOwnerId());
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public LedgerAccountResponse getAccount(String ownerId) {
        return toAccountResponse(getAccountByOwner(ownerId));
    }

    // ── Double-entry transaction posting ──────────────────────────

    /**
     * Posts a balanced double-entry transaction:
     *   - DEBIT the debitOwner's account (reduces their balance)
     *   - CREDIT the creditOwner's account (increases their balance)
     *
     * Debits and Credits always balance — this is the core invariant.
     */
    public LedgerTransactionResponse postTransaction(PostTransactionRequest req) {
        // Idempotency: don't post the same external transaction twice
        if (req.getExternalRef() != null && txRepo.existsByExternalRef(req.getExternalRef())) {
            return toTxResponse(txRepo.findByExternalRef(req.getExternalRef()).get());
        }

        LedgerAccount debitAccount  = getOrCreateUserAccount(req.getDebitOwnerId());
        LedgerAccount creditAccount = getOrCreateUserAccount(req.getCreditOwnerId());

        // Check debit account has sufficient balance (skip for system accounts)
        if (debitAccount.getType() == LedgerAccount.AccountType.USER_WALLET
                || debitAccount.getType() == LedgerAccount.AccountType.MERCHANT) {
            if (debitAccount.getBalance().compareTo(req.getAmount()) < 0) {
                throw new LedgerException("Insufficient ledger balance for account: " + debitAccount.getId());
            }
        }

        // Update balances
        debitAccount.setBalance(debitAccount.getBalance().subtract(req.getAmount()));
        creditAccount.setBalance(creditAccount.getBalance().add(req.getAmount()));
        accountRepo.save(debitAccount);
        accountRepo.save(creditAccount);

        // Record the transaction header
        LedgerTransaction tx = txRepo.save(LedgerTransaction.builder()
                .amount(req.getAmount())
                .category(req.getCategory())
                .debitAccountId(debitAccount.getId())
                .creditAccountId(creditAccount.getId())
                .description(req.getDescription())
                .externalRef(req.getExternalRef())
                .initiatedBy(req.getInitiatedBy())
                .build());

        // Record two balanced ledger entries
        entryRepo.save(LedgerEntry.builder()
                .transactionRef(tx.getId())
                .account(debitAccount)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(req.getAmount())
                .runningBalance(debitAccount.getBalance())
                .category(req.getCategory())
                .description(req.getDescription())
                .externalRef(req.getExternalRef())
                .counterAccountId(creditAccount.getId())
                .build());

        entryRepo.save(LedgerEntry.builder()
                .transactionRef(tx.getId())
                .account(creditAccount)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(req.getAmount())
                .runningBalance(creditAccount.getBalance())
                .category(req.getCategory())
                .description(req.getDescription())
                .externalRef(req.getExternalRef())
                .counterAccountId(debitAccount.getId())
                .build());

        log.info("Posted ledger tx {}: R{} DEBIT {} CREDIT {} [{}]",
                tx.getId(), req.getAmount(), debitAccount.getId(), creditAccount.getId(), req.getCategory());
        return toTxResponse(tx);
    }

    /**
     * Reverses a posted transaction by creating an equal and opposite entry pair.
     */
    public LedgerTransactionResponse reverseTransaction(String txId) {
        LedgerTransaction original = txRepo.findById(txId)
                .orElseThrow(() -> new LedgerException("Transaction not found: " + txId));

        if (original.getStatus() == LedgerTransaction.TransactionStatus.REVERSED) {
            throw new LedgerException("Transaction already reversed: " + txId);
        }

        // Post a reverse — swap debit/credit
        PostTransactionRequest reversal = new PostTransactionRequest();
        reversal.setDebitOwnerId(getOwnerIdByAccountId(original.getCreditAccountId()));
        reversal.setCreditOwnerId(getOwnerIdByAccountId(original.getDebitAccountId()));
        reversal.setAmount(original.getAmount());
        reversal.setCategory(LedgerEntry.EntryCategory.REVERSAL);
        reversal.setDescription("Reversal of tx: " + txId);
        reversal.setExternalRef("REV-" + txId);
        postTransaction(reversal);

        original.setStatus(LedgerTransaction.TransactionStatus.REVERSED);
        txRepo.save(original);

        log.info("Reversed ledger transaction {}", txId);
        return toTxResponse(original);
    }

    // ── Statements & history ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getEntries(String ownerId, int page, int size) {
        LedgerAccount account = getAccountByOwner(ownerId);
        return entryRepo.findByAccountIdOrderByCreatedAtDesc(account.getId(), PageRequest.of(page, size))
                .stream().map(this::toEntryResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getStatement(String ownerId, LocalDateTime from, LocalDateTime to) {
        LedgerAccount account = getAccountByOwner(ownerId);
        List<LedgerEntry> entries = entryRepo.findByAccountAndDateRange(account.getId(), from, to);

        BigDecimal totalCredits = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Opening balance = current balance - credits + debits in period
        BigDecimal openingBalance = account.getBalance().subtract(totalCredits).add(totalDebits);

        return AccountStatementResponse.builder()
                .accountId(account.getId())
                .ownerId(ownerId)
                .currency(account.getCurrency())
                .openingBalance(openingBalance)
                .closingBalance(account.getBalance())
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .from(from).to(to)
                .entries(entries.stream().map(this::toEntryResponse).collect(Collectors.toList()))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private LedgerAccount getAccountByOwner(String ownerId) {
        return accountRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new LedgerException("No ledger account for owner: " + ownerId));
    }

    private LedgerAccount getOrCreateUserAccount(String ownerId) {
        return accountRepo.findByOwnerId(ownerId).orElseGet(() ->
                accountRepo.save(LedgerAccount.builder()
                        .ownerId(ownerId)
                        .type(LedgerAccount.AccountType.USER_WALLET)
                        .build()));
    }

    private String getOwnerIdByAccountId(String accountId) {
        return accountRepo.findById(accountId)
                .map(LedgerAccount::getOwnerId)
                .orElseThrow(() -> new LedgerException("Account not found: " + accountId));
    }

    // ── Mappers ───────────────────────────────────────────────────

    private LedgerAccountResponse toAccountResponse(LedgerAccount a) {
        return LedgerAccountResponse.builder()
                .id(a.getId()).ownerId(a.getOwnerId()).type(a.getType())
                .currency(a.getCurrency()).balance(a.getBalance())
                .status(a.getStatus()).description(a.getDescription())
                .createdAt(a.getCreatedAt()).build();
    }

    private LedgerEntryResponse toEntryResponse(LedgerEntry e) {
        return LedgerEntryResponse.builder()
                .id(e.getId()).transactionRef(e.getTransactionRef())
                .accountId(e.getAccount().getId()).entryType(e.getEntryType())
                .amount(e.getAmount()).runningBalance(e.getRunningBalance())
                .category(e.getCategory()).description(e.getDescription())
                .externalRef(e.getExternalRef()).counterAccountId(e.getCounterAccountId())
                .createdAt(e.getCreatedAt()).build();
    }

    private LedgerTransactionResponse toTxResponse(LedgerTransaction t) {
        return LedgerTransactionResponse.builder()
                .id(t.getId()).amount(t.getAmount()).currency(t.getCurrency())
                .category(t.getCategory()).status(t.getStatus())
                .debitAccountId(t.getDebitAccountId()).creditAccountId(t.getCreditAccountId())
                .description(t.getDescription()).externalRef(t.getExternalRef())
                .initiatedBy(t.getInitiatedBy()).createdAt(t.getCreatedAt()).build();
    }
}
