package com.safipay.merchant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MerchantResponse {
    private UUID merchantId;
    private String name;
    private String email;
    private String category;
    private String status;
    private UUID walletId;
}
