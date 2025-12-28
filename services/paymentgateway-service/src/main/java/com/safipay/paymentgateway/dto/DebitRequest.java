package com.safipay.paymentgateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class DebitRequest {
    private UUID walletId;
    private BigDecimal amount;
}
