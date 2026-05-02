package com.safipay.webhook.controller;

import com.safipay.webhook.dto.request.RegisterEndpointRequest;
import com.safipay.webhook.dto.response.*;
import com.safipay.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/endpoints")
    public ResponseEntity<ApiResponse<WebhookEndpointResponse>> register(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody RegisterEndpointRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Endpoint registered", webhookService.registerEndpoint(userId, req)));
    }

    @GetMapping("/endpoints")
    public ResponseEntity<ApiResponse<List<WebhookEndpointResponse>>> getEndpoints(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(webhookService.getMyEndpoints(userId)));
    }

    @PostMapping("/endpoints/{endpointId}/pause")
    public ResponseEntity<ApiResponse<Void>> pause(
            @PathVariable String endpointId, @AuthenticationPrincipal String userId) {
        webhookService.pauseEndpoint(endpointId, userId);
        return ResponseEntity.ok(ApiResponse.success("Endpoint paused", null));
    }

    @DeleteMapping("/endpoints/{endpointId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String endpointId, @AuthenticationPrincipal String userId) {
        webhookService.deleteEndpoint(endpointId, userId);
        return ResponseEntity.ok(ApiResponse.success("Endpoint removed", null));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<WebhookEventResponse>>> getEvents(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(webhookService.getMyEvents(userId)));
    }

    @GetMapping("/events/{eventId}/deliveries")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryResponse>>> getDeliveries(
            @PathVariable String eventId) {
        return ResponseEntity.ok(ApiResponse.success(webhookService.getDeliveries(eventId)));
    }
}
