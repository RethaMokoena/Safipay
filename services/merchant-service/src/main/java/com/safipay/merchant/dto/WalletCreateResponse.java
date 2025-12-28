package com.safipay.merchant.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class WalletCreateResponse {
    private UUID walletId;
}
