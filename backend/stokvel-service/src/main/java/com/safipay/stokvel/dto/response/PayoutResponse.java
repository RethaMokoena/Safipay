package com.safipay.stokvel.dto.response;
import com.safipay.stokvel.model.Payout;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;

@Data @Builder
public class PayoutResponse {
    private Long id; private Long stokvelId; private String recipientUserId;
    private BigDecimal amount; private Payout.PayoutStatus status; private Payout.PayoutType type;
    private Integer cycleNumber; private LocalDateTime processedAt;
}
