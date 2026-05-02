package com.safipay.webhook.dto.response;
import com.safipay.webhook.model.WebhookEndpoint;
import lombok.*; import java.time.LocalDateTime; import java.util.List;

@Data @Builder
public class WebhookEndpointResponse {
    private String id; private String ownerId; private String targetUrl;
    private List<String> subscribedEvents;
    private WebhookEndpoint.EndpointStatus status;
    private Integer successCount; private Integer failureCount;
    private LocalDateTime lastDeliveryAt; private LocalDateTime createdAt;
}
