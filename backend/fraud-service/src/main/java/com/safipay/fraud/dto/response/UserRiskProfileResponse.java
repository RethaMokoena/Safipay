package com.safipay.fraud.dto.response;
import com.safipay.fraud.model.UserRiskProfile;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;

@Data @Builder
public class UserRiskProfileResponse {
    private String userId; private UserRiskProfile.RiskTier riskTier;
    private BigDecimal avgTransactionAmount; private Integer totalTransactions;
    private BigDecimal totalTransactionVolume;
    private Integer transactionsLastHour; private BigDecimal amountLastHour;
    private BigDecimal amountLastDay; private Boolean isBlacklisted;
    private Integer openAlertCount; private LocalDateTime lastTransactionAt;
    private LocalDateTime updatedAt;
}
