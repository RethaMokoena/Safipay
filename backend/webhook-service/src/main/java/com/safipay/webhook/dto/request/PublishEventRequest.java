package com.safipay.webhook.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PublishEventRequest {
    @NotBlank private String eventType;
    @NotBlank private String ownerId;
    @NotBlank private String payload;    // JSON string
    private String relatedEntityId;
}
