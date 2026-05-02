package com.safipay.stokvel.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Component @Slf4j
public class WebhookClient {

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${services.webhook-service-url:http://localhost:8088}") private String webhookUrl;

    @Data static class WebhookEventRequest {
        private String eventType; private String ownerId;
        private String payload; private String relatedEntityId;
    }

    public void fireEvent(String eventType, String userId, String entityId, String payloadJson) {
        try {
            WebhookEventRequest req = new WebhookEventRequest();
            req.setEventType(eventType); req.setOwnerId(userId);
            req.setPayload(payloadJson); req.setRelatedEntityId(entityId);
            restTemplate.postForEntity(webhookUrl + "/internal/webhooks/events", req, Object.class);
        } catch (Exception e) {
            log.warn("Webhook service unavailable: {}", e.getMessage());
        }
    }
}
