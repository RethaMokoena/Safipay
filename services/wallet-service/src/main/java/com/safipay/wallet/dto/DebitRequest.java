package com.safipay.wallet.dto;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
@Data
public class DebitRequest {
    private UUID walletId;
    private BigDecimal amount;
}
