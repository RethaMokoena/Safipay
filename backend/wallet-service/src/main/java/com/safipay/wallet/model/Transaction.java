package com.safipay.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionStatus status;

    private String referenceId;    // external payment/stokvel ID
    private String description;
    private String counterpartyUserId;

    @CreationTimestamp private LocalDateTime createdAt;

    public enum TransactionType { CREDIT, DEBIT }
    public enum TransactionStatus { PENDING, COMPLETED, FAILED, REVERSED }
}
