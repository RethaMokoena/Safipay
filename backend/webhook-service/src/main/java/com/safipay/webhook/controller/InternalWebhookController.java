package com.safipay.webhook.controller;

import com.safipay.webhook.dto.request.PublishEventRequest;
import com.safipay.webhook.dto.response.*;
import com.safipay.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/webhooks")
@RequiredArgsConstructor
public class InternalWebhookController {

    private final WebhookService webhookService;

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<WebhookEventResponse>> publish(
            @Valid @RequestBody PublishEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event published", webhookService.publishEvent(req)));
    }
}
