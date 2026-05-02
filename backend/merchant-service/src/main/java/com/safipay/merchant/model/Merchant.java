package com.safipay.merchant.model;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;

@Entity @Table(name = "merchants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Merchant {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Owner/operator of this merchant account (user-service userId)
    @Column(nullable = false)
    private String ownerUserId;

    @Column(nullable = false)
    private String businessName;

    @Column(unique = true)
    private String businessRegistrationNumber;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BusinessCategory category;

    @Column
    private String businessEmail;

    @Column
    private String businessPhone;

    @Column(length = 500)
    private String description;

    @Column
    private String logoUrl;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private MerchantStatus status = MerchantStatus.PENDING_VERIFICATION;

    // Linked wallet for receiving payments
    @Column
    private String walletId;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum BusinessCategory {
        RETAIL, FOOD_BEVERAGE, HEALTH_BEAUTY, TRANSPORT,
        EDUCATION, ENTERTAINMENT, SERVICES, UTILITIES, OTHER
    }

    public enum MerchantStatus {
        PENDING_VERIFICATION, ACTIVE, SUSPENDED, REJECTED, CLOSED
    }
}
