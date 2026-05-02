package com.safipay.wallet.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "Recipient user ID is required")
    private String recipientUserId;
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    private String description;
}
