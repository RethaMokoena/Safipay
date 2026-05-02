package com.safipay.ledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Groups two or more LedgerEntry records into a balanced double-entry transaction.
 * debits must equal credits for every transaction.
 */
@Entity
@Table(name = "ledger_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerTransaction {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "ZAR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntry.EntryCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.POSTED;

    @Column(nullable = false)
    private String debitAccountId;

    @Column(nullable = false)
    private String creditAccountId;

    @Column
    private String description;

    // ID from external service (payment-service, stokvel-service, etc.)
    @Column
    private String externalRef;

    // User who initiated the transaction
    @Column
    private String initiatedBy;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum TransactionStatus { PENDING, POSTED, REVERSED, FAILED }
}
