package com.safipay.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Table;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "ZAR";

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public BigDecimal getAvailableBalance() { return balance.subtract(lockedBalance); }
    public enum WalletStatus { ACTIVE, FROZEN, CLOSED }
}
