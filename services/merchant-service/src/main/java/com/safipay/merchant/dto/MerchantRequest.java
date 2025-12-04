package com.safipay.merchant.dto;

import lombok.Data;

@Data
public class MerchantRequest {
    private String name;
    private String email;
    private String category;
}
