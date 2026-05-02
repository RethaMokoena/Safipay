package com.safipay.stokvel.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ContributeRequest {
    @NotNull @Positive private BigDecimal amount;
    @NotBlank private String transactionId;
}
