package com.safipay.wallet.dto.response;
import com.safipay.wallet.model.Transaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class TransactionResponse {
    private String id;
    private String walletId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Transaction.TransactionType type;
    private Transaction.TransactionStatus status;
    private String referenceId;
    private String description;
    private String counterpartyUserId;
    private LocalDateTime createdAt;
}
