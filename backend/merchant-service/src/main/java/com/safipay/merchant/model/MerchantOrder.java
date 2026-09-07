package com.safipay.merchant.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "checkout_id",
        nullable = false
    )
    private MarketplaceCheckout checkout;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "merchant_id",
        nullable = false
    )
    private Merchant merchant;

    @Column(nullable = false)
    private String buyerUserId;

    @Column(
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal totalAmount;

    /*
     * Set after the existing SafiPay
     * merchant payment succeeds.
     */
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status =
        OrderStatus.PENDING_PAYMENT;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING_PAYMENT,
        PAID,
        PAYMENT_FAILED,
        PROCESSING,
        COMPLETED,
        CANCELLED,
        REFUNDED
    }
}