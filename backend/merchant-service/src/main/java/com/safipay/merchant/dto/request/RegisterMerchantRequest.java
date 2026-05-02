package com.safipay.merchant.dto.request;
import com.safipay.merchant.model.Merchant;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterMerchantRequest {
    @NotBlank @Size(min=2,max=120) private String businessName;
    @NotNull private Merchant.BusinessCategory category;
    private String businessRegistrationNumber;
    @Email private String businessEmail;
    private String businessPhone;
    @Size(max=500) private String description;
}
