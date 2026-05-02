package com.safipay.stokvel.dto.request;
import com.safipay.stokvel.model.Stokvel;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateStokvelRequest {
    @NotBlank @Size(min=3,max=100) private String name;
    @Size(max=500) private String description;
    @NotNull private Stokvel.StokvelType type;
    @NotNull @DecimalMin("1.00") private BigDecimal contributionAmount;
    @NotNull private Stokvel.ContributionFrequency contributionFrequency;
    @NotNull @Min(2) @Max(100) private Integer maxMembers;
}
