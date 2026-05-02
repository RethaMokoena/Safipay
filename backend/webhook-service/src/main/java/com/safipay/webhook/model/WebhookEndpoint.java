package com.safipay.webhook.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * A registered webhook endpoint — a URL a user or merchant wants called
 * when specific SafiPay events occur.
 */
@Entity @Table(name="webhook_endpoints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookEndpoint {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String ownerId;     // userId or merchantId
    @Column(nullable = false) private String targetUrl;   // HTTPS endpoint to call
    @Column(nullable = false) private String secretKey;   // HMAC-SHA256 signing secret

    // Comma-separated list of subscribed event types
    @Column(nullable = false, length = 1000)
    private String subscribedEvents;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private EndpointStatus status = EndpointStatus.ACTIVE;

    @Column @Builder.Default private Integer successCount = 0;
    @Column @Builder.Default private Integer failureCount = 0;
    @Column private LocalDateTime lastDeliveryAt;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum EndpointStatus { ACTIVE, PAUSED, DISABLED }
}
