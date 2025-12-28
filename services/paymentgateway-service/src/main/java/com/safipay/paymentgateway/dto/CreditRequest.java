package com.safipay.paymentgateway.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreditRequest {
    private UUID walletId;
    private BigDecimal amount;

    public CreditRequest(UUID walletId, BigDecimal amount) {
        this.walletId = walletId;
        this.amount = amount;
    }
}
