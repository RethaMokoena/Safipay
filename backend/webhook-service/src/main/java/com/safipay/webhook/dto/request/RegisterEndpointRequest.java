package com.safipay.webhook.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class RegisterEndpointRequest {
    @NotBlank @Pattern(regexp="https://.*", message="Target URL must use HTTPS")
    private String targetUrl;
    @NotEmpty
    private List<String> subscribedEvents; // e.g. ["payment.completed","stokvel.payout"]
}
