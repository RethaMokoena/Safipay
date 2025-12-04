package com.safipay.fraud.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class FraudCheckRequest {
    private UUID walletId;
    private BigDecimal amount;
    private String paymentType;
}
