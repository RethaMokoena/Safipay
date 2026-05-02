package com.safipay.webhook.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * A single delivery attempt for a WebhookEvent to a WebhookEndpoint.
 * Tracks status, response, retries for observability.
 */
@Entity @Table(name="webhook_deliveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookDelivery {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private WebhookEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column private Integer httpStatusCode;
    @Column(length = 2000) private String responseBody;
    @Column(length = 1000) private String errorMessage;
    @Column @Builder.Default private Integer attemptCount = 0;
    @Column private LocalDateTime nextRetryAt;

    @CreationTimestamp private LocalDateTime attemptedAt;

    public enum DeliveryStatus { PENDING, SUCCESS, FAILED, RETRYING, EXHAUSTED }
}
