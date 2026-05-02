package com.safipay.merchant.dto.response;
import com.safipay.merchant.model.MerchantApiKey;
import lombok.*; import java.time.LocalDateTime;

@Data @Builder
public class ApiKeyResponse {
    private String id; private String merchantId;
    private String keyPrefix;
    // Only returned once on creation — never again
    private String fullKey;
    private String label;
    private MerchantApiKey.KeyEnvironment environment;
    private Boolean active; private LocalDateTime expiresAt; private LocalDateTime createdAt;
}
