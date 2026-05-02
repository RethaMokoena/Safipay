package com.safipay.webhook.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * An event that needs to be delivered to all matching webhook endpoints.
 * Events are persisted for retry and audit.
 */
@Entity @Table(name="webhook_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookEvent {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String eventType;   // e.g. payment.completed
    @Column(nullable = false) private String ownerId;     // who this event belongs to
    @Column(nullable = false, length = 5000) private String payload; // JSON
    @Column private String relatedEntityId;               // paymentId, stokvelId etc.

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private EventStatus status = EventStatus.PENDING;

    @CreationTimestamp private LocalDateTime createdAt;

    public enum EventStatus { PENDING, PROCESSING, DELIVERED, FAILED }
}
