package com.safipay.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SendMoneyRequest {

    @Email
    private String recipientEmail;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    private String description;
    private String referenceNote;
}
