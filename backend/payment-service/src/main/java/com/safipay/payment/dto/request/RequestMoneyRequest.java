package com.safipay.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RequestMoneyRequest {
    @NotBlank  private String fromUserId;   // who to request from
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    private String description;
    private String referenceNote;
}
