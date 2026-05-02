package com.safipay.webhook.dto.response;
import com.safipay.webhook.model.WebhookDelivery;
import lombok.*; import java.time.LocalDateTime;

@Data @Builder
public class WebhookDeliveryResponse {
    private String id; private String eventId; private String endpointId;
    private WebhookDelivery.DeliveryStatus status;
    private Integer httpStatusCode; private String errorMessage;
    private Integer attemptCount; private LocalDateTime attemptedAt;
}
