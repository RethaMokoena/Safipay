package com.safipay.ledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single line in the double-entry ledger.
 * Every financial event produces exactly 2 entries (one DEBIT + one CREDIT) that sum to zero.
 */
@Entity
@Table(name = "ledger_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // The transaction this entry belongs to (groups the 2 sides of a transfer)
    @Column(nullable = false)
    private String transactionRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;  // DEBIT or CREDIT

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal runningBalance;  // account balance after this entry

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryCategory category;

    @Column
    private String description;

    // External reference: payment ID, stokvel contribution ID, etc.
    @Column
    private String externalRef;

    // The other account in this double-entry pair
    @Column
    private String counterAccountId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum EntryType { DEBIT, CREDIT }

    public enum EntryCategory {
        TRANSFER,           // Peer-to-peer transfer
        TOP_UP,             // Wallet top-up (external funding)
        WITHDRAWAL,         // Wallet withdrawal to bank
        STOKVEL_CONTRIBUTION,
        STOKVEL_PAYOUT,
        MERCHANT_PAYMENT,
        FEE,                // Platform fee
        REVERSAL,           // Reversed transaction
        ADJUSTMENT          // Manual ledger correction
    }
}
