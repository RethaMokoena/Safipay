package com.safipay.wallet.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TopUpRequest {
    @NotNull @DecimalMin("1.00")
    private BigDecimal amount;
    private String referenceId;
}
