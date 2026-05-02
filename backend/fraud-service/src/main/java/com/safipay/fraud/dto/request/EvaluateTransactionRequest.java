package com.safipay.fraud.dto.request;
import com.safipay.fraud.model.FraudEvaluation;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class EvaluateTransactionRequest {
    @NotBlank  private String transactionRef;
    @NotBlank  private String userId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotNull   private FraudEvaluation.TransactionType transactionType;
    private String recipientUserId;
    private String merchantId;
    private String ipAddress;
    private String deviceId;
}
