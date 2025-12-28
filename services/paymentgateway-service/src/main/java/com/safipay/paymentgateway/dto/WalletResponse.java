package com.safipay.paymentgateway.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WalletResponse {
    private UUID walletId;
    private BigDecimal balance;
}
