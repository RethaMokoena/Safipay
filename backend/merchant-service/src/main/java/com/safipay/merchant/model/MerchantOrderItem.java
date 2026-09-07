package com.safipay.merchant.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "order_id",
        nullable = false
    )
    private MerchantOrder order;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "listing_id",
        nullable = false
    )
    private MerchantListing listing;

    /*
     * Historical snapshot.
     *
     * We do not rely on the listing title
     * later because the merchant may edit it.
     */
    @Column(
        nullable = false,
        length = 150
    )
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantListing.ListingType type;

    @Column(nullable = false)
    private Integer quantity;

    /*
     * Price at the exact time of checkout.
     */
    @Column(
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}