package com.safipay.wallet.dto;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
@Data
public class TransferRequest {
    private UUID fromWalletId;
    private UUID toWalletId;
    private BigDecimal amount;
}

