package com.safipay.paymentgateway.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class MerchantInfoResponse {
    private UUID merchantId;
    private UUID walletId;
    private String name;
    private String email;
    private String status;
}
