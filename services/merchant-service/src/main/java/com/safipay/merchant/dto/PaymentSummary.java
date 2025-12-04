package com.safipay.merchant.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentSummary {
    private UUID paymentId;
    private BigDecimal amount;
    private Instant createdAt;
    private String status;
}
