package com.safipay.merchant.dto.request;
import com.safipay.merchant.model.MerchantApiKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateApiKeyRequest {
    @NotBlank private String label;
    @NotNull  private MerchantApiKey.KeyEnvironment environment;
    private LocalDateTime expiresAt;
}
