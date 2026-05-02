package com.safipay.ledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a double-entry ledger account for a user or system entity.
 * Every money movement creates two entries: a DEBIT on one account and a CREDIT on another.
 */
@Entity
@Table(name = "ledger_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerAccount {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Links to user-service userId, merchant ID, or system account name
    @Column(nullable = false, unique = true)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "ZAR";

    // Sum of all CREDIT entries - Sum of all DEBIT entries
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column
    private String description;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum AccountType {
        USER_WALLET,    // Regular user wallet
        MERCHANT,       // Merchant account
        FEE_POOL,       // SafiPay revenue account
        SUSPENSE,       // Temporary holding during processing
        FLOAT           // System float / liquidity account
    }

    public enum AccountStatus { ACTIVE, FROZEN, CLOSED }
}
