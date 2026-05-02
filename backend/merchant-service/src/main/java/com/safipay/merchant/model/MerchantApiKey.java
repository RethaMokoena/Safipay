package com.safipay.merchant.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * API keys issued to merchants for server-to-server payment requests.
 * Keys are hashed before storage — never stored in plain text.
 */
@Entity @Table(name = "merchant_api_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantApiKey {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, unique = true)
    private String keyPrefix;   // First 8 chars shown to merchant (e.g. "sp_live_")

    @Column(nullable = false)
    private String keyHash;     // BCrypt hash of the full key

    @Column(nullable = false)
    private String label;       // Human name ("Production", "Test")

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private KeyEnvironment environment = KeyEnvironment.TEST;

    @Column(nullable = false) @Builder.Default
    private Boolean active = true;

    @Column
    private LocalDateTime lastUsedAt;

    @Column
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum KeyEnvironment { TEST, LIVE }
}
