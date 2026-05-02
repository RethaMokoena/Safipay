package com.safipay.webhook.service;

import com.safipay.webhook.dto.request.PublishEventRequest;
import com.safipay.webhook.dto.request.RegisterEndpointRequest;
import com.safipay.webhook.dto.response.*;
import com.safipay.webhook.exception.WebhookException;
import com.safipay.webhook.model.*;
import com.safipay.webhook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WebhookService {

    private final WebhookEndpointRepository endpointRepo;
    private final WebhookEventRepository eventRepo;
    private final WebhookDeliveryRepository deliveryRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${webhook.max-retry-attempts:3}")   private int maxRetries;
    @Value("${webhook.retry-delay-seconds:30}") private int retryDelaySecs;
    @Value("${webhook.timeout-seconds:10}")     private int timeoutSecs;
    @Value("${webhook.signature-header:X-SafiPay-Signature}") private String sigHeader;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Endpoint Management ───────────────────────────────────────

    public WebhookEndpointResponse registerEndpoint(String ownerId, RegisterEndpointRequest req) {
        String secretKey = generateSecret();

        WebhookEndpoint endpoint = endpointRepo.save(WebhookEndpoint.builder()
                .ownerId(ownerId)
                .targetUrl(req.getTargetUrl())
                .secretKey(secretKey)
                .subscribedEvents(String.join(",", req.getSubscribedEvents()))
                .build());

        log.info("Registered webhook endpoint {} for owner {}", endpoint.getId(), ownerId);
        return toEndpointResponse(endpoint, secretKey);
    }

    public void pauseEndpoint(String endpointId, String ownerId) {
        WebhookEndpoint endpoint = getEndpointOrThrow(endpointId);
        assertOwner(endpoint, ownerId);
        endpoint.setStatus(WebhookEndpoint.EndpointStatus.PAUSED);
        endpointRepo.save(endpoint);
    }

    public void deleteEndpoint(String endpointId, String ownerId) {
        WebhookEndpoint endpoint = getEndpointOrThrow(endpointId);
        assertOwner(endpoint, ownerId);
        endpoint.setStatus(WebhookEndpoint.EndpointStatus.DISABLED);
        endpointRepo.save(endpoint);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> getMyEndpoints(String ownerId) {
        return endpointRepo.findByOwnerIdAndStatus(ownerId, WebhookEndpoint.EndpointStatus.ACTIVE)
                .stream().map(e -> toEndpointResponse(e, null)).collect(Collectors.toList());
    }

    // ── Event Publishing ──────────────────────────────────────────

    /**
     * Called by other services (internally) to fire an event.
     * Creates the event record, finds matching endpoints, dispatches async.
     */
    public WebhookEventResponse publishEvent(PublishEventRequest req) {
        WebhookEvent event = eventRepo.save(WebhookEvent.builder()
                .eventType(req.getEventType())
                .ownerId(req.getOwnerId())
                .payload(req.getPayload())
                .relatedEntityId(req.getRelatedEntityId())
                .build());

        // Find all active endpoints subscribed to this event type
        List<WebhookEndpoint> matchingEndpoints =
                endpointRepo.findActiveEndpointsForEvent(req.getEventType());

        for (WebhookEndpoint endpoint : matchingEndpoints) {
            WebhookDelivery delivery = deliveryRepo.save(WebhookDelivery.builder()
                    .event(event).endpoint(endpoint).build());
            dispatchAsync(event, endpoint, delivery);
        }

        log.info("Event {} published to {} endpoints", event.getEventType(), matchingEndpoints.size());
        return toEventResponse(event);
    }

    // ── Async delivery ────────────────────────────────────────────

    @Async
    public void dispatchAsync(WebhookEvent event, WebhookEndpoint endpoint, WebhookDelivery delivery) {
        attemptDelivery(event, endpoint, delivery);
    }

    private void attemptDelivery(WebhookEvent event, WebhookEndpoint endpoint, WebhookDelivery delivery) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setStatus(WebhookDelivery.DeliveryStatus.PENDING);

        try {
            String signature = computeSignature(event.getPayload(), endpoint.getSecretKey());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(sigHeader, "sha256=" + signature);
            headers.set("X-SafiPay-Event", event.getEventType());
            headers.set("X-SafiPay-Delivery", delivery.getId());
            headers.set("X-SafiPay-Timestamp", String.valueOf(System.currentTimeMillis()));

            HttpEntity<String> httpEntity = new HttpEntity<>(event.getPayload(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint.getTargetUrl(), httpEntity, String.class);

            int status = response.getStatusCode().value();
            delivery.setHttpStatusCode(status);
            delivery.setResponseBody(response.getBody() != null
                    ? response.getBody().substring(0, Math.min(response.getBody().length(), 500)) : "");

            if (status >= 200 && status < 300) {
                delivery.setStatus(WebhookDelivery.DeliveryStatus.SUCCESS);
                endpoint.setSuccessCount(endpoint.getSuccessCount() + 1);
                endpoint.setLastDeliveryAt(LocalDateTime.now());
                event.setStatus(WebhookEvent.EventStatus.DELIVERED);
                log.debug("Webhook delivered to {} — HTTP {}", endpoint.getTargetUrl(), status);
            } else {
                handleFailure(delivery, endpoint, "Non-2xx response: " + status);
            }

        } catch (Exception e) {
            handleFailure(delivery, endpoint, e.getMessage());
        }

        deliveryRepo.save(delivery);
        endpointRepo.save(endpoint);
        eventRepo.save(event);
    }

    private void handleFailure(WebhookDelivery delivery, WebhookEndpoint endpoint, String error) {
        delivery.setErrorMessage(error != null ? error.substring(0, Math.min(error.length(), 500)) : "Unknown error");
        endpoint.setFailureCount(endpoint.getFailureCount() + 1);

        if (delivery.getAttemptCount() < maxRetries) {
            delivery.setStatus(WebhookDelivery.DeliveryStatus.RETRYING);
            delivery.setNextRetryAt(LocalDateTime.now().plusSeconds(retryDelaySecs * delivery.getAttemptCount()));
            log.warn("Webhook delivery failed (attempt {}), scheduled retry at {}",
                    delivery.getAttemptCount(), delivery.getNextRetryAt());
        } else {
            delivery.setStatus(WebhookDelivery.DeliveryStatus.EXHAUSTED);
            log.error("Webhook delivery exhausted after {} attempts to {}", maxRetries, endpoint.getTargetUrl());
        }
    }

    // ── Retry scheduler ───────────────────────────────────────────

    @Scheduled(fixedDelay = 30000) // every 30 seconds
    public void retryFailedDeliveries() {
        List<WebhookDelivery> due = deliveryRepo.findByStatusAndNextRetryAtBefore(
                WebhookDelivery.DeliveryStatus.RETRYING, LocalDateTime.now());

        if (!due.isEmpty()) {
            log.info("Retrying {} failed webhook deliveries", due.size());
        }

        for (WebhookDelivery delivery : due) {
            attemptDelivery(delivery.getEvent(), delivery.getEndpoint(), delivery);
        }
    }

    // ── Queries ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WebhookEventResponse> getMyEvents(String ownerId) {
        return eventRepo.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(this::toEventResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> getDeliveries(String eventId) {
        return deliveryRepo.findByEventId(eventId)
                .stream().map(this::toDeliveryResponse).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String computeSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new WebhookException("Failed to compute signature: " + e.getMessage());
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private WebhookEndpoint getEndpointOrThrow(String id) {
        return endpointRepo.findById(id)
                .orElseThrow(() -> new WebhookException("Endpoint not found: " + id));
    }

    private void assertOwner(WebhookEndpoint endpoint, String ownerId) {
        if (!endpoint.getOwnerId().equals(ownerId))
            throw new WebhookException("You do not own this endpoint");
    }

    // ── Mappers ───────────────────────────────────────────────────

    private WebhookEndpointResponse toEndpointResponse(WebhookEndpoint e, String plainSecret) {
        var response = WebhookEndpointResponse.builder()
                .id(e.getId()).ownerId(e.getOwnerId()).targetUrl(e.getTargetUrl())
                .subscribedEvents(Arrays.asList(e.getSubscribedEvents().split(",")))
                .status(e.getStatus()).successCount(e.getSuccessCount())
                .failureCount(e.getFailureCount()).lastDeliveryAt(e.getLastDeliveryAt())
                .createdAt(e.getCreatedAt()).build();
        return response;
    }

    private WebhookEventResponse toEventResponse(WebhookEvent e) {
        return WebhookEventResponse.builder()
                .id(e.getId()).eventType(e.getEventType()).ownerId(e.getOwnerId())
                .payload(e.getPayload()).relatedEntityId(e.getRelatedEntityId())
                .status(e.getStatus()).createdAt(e.getCreatedAt()).build();
    }

    private WebhookDeliveryResponse toDeliveryResponse(WebhookDelivery d) {
        return WebhookDeliveryResponse.builder()
                .id(d.getId()).eventId(d.getEvent().getId()).endpointId(d.getEndpoint().getId())
                .status(d.getStatus()).httpStatusCode(d.getHttpStatusCode())
                .errorMessage(d.getErrorMessage()).attemptCount(d.getAttemptCount())
                .attemptedAt(d.getAttemptedAt()).build();
    }
}
