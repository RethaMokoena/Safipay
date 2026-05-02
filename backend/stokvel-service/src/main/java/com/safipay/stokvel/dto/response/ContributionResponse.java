package com.safipay.stokvel.dto.response;
import com.safipay.stokvel.model.Contribution;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;

@Data @Builder
public class ContributionResponse {
    private Long id; private Long stokvelId; private String userId;
    private BigDecimal amount; private Contribution.ContributionStatus status;
    private String transactionId; private Integer cycleNumber; private LocalDateTime contributedAt;
}
