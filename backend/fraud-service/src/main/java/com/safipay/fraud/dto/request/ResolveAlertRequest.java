package com.safipay.fraud.dto.request;
import com.safipay.fraud.model.FraudAlert;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveAlertRequest {
    @NotNull private FraudAlert.AlertStatus resolution;
    private String notes;
}
