package com.safipay.webhook.dto.response;
import com.safipay.webhook.model.WebhookEvent;
import lombok.*; import java.time.LocalDateTime;

@Data @Builder
public class WebhookEventResponse {
    private String id; private String eventType; private String ownerId;
    private String payload; private String relatedEntityId;
    private WebhookEvent.EventStatus status; private LocalDateTime createdAt;
}
