package com.safipay.merchant.dto.response;
import com.safipay.merchant.model.Merchant;
import lombok.*; import java.time.LocalDateTime;

@Data @Builder
public class MerchantResponse {
    private String id; private String ownerUserId;
    private String businessName; private String businessRegistrationNumber;
    private Merchant.BusinessCategory category;
    private String businessEmail; private String businessPhone;
    private String description; private String logoUrl;
    private Merchant.MerchantStatus status; private String walletId;
    private LocalDateTime createdAt;
}
