package com.safipay.wallet.dto.response;
import com.safipay.wallet.model.Wallet;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class WalletResponse {
    private String id;
    private String userId;
    private BigDecimal balance;
    private BigDecimal lockedBalance;
    private BigDecimal availableBalance;
    private String currency;
    private Wallet.WalletStatus status;
    private LocalDateTime createdAt;
}
