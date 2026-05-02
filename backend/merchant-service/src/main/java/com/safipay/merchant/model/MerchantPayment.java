package com.safipay.merchant.model;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A payment made TO a merchant by a SafiPay user.
 */
@Entity @Table(name = "merchant_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantPayment {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false)
    private String payerUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3) @Builder.Default
    private String currency = "ZAR";

    @Column
    private String description;

    // Merchant's own reference (e.g. order ID)
    @Column
    private String merchantReference;

    // SafiPay payment-service transaction ID
    @Column
    private String paymentTransactionId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // Merchant receives amount minus fee
    @Column(precision = 19, scale = 4) @Builder.Default
    private BigDecimal feePercentage = new BigDecimal("0.015"); // 1.5%

    @Column(precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal netAmount;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }
}
