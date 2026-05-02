package com.safipay.fraud.dto.response;
import com.safipay.fraud.model.FraudAlert;
import lombok.*; import java.time.LocalDateTime;

@Data @Builder
public class FraudAlertResponse {
    private String id; private String evaluationId;
    private String userId; private FraudAlert.AlertType alertType;
    private FraudAlert.AlertStatus status; private String description;
    private String resolvedBy; private String resolutionNotes;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
