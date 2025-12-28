package com.safipay.paymentgateway.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID userId;
    private UUID payerWalletId;
    private UUID merchantWalletId; // internal merchant wallet; else use merchantId+webhook
    private String merchantId;
    private BigDecimal amount;
}
