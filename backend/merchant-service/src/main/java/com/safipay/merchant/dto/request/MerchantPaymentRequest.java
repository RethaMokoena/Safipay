package com.safipay.merchant.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MerchantPaymentRequest {
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotBlank private String payerUserId;
    private String description;
    private String merchantReference;  // idempotency key
}
