package com.safipay.stokvel.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PoolWithdrawalRequest {
    @NotNull @Positive private BigDecimal amount;
    private String notes;
}
