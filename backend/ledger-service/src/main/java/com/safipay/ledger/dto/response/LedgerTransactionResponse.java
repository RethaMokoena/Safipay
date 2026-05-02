package com.safipay.ledger.dto.response;
import com.safipay.ledger.model.*;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;

@Data @Builder
public class LedgerTransactionResponse {
    private String id; private BigDecimal amount; private String currency;
    private LedgerEntry.EntryCategory category; private LedgerTransaction.TransactionStatus status;
    private String debitAccountId; private String creditAccountId;
    private String description; private String externalRef;
    private String initiatedBy; private LocalDateTime createdAt;
}
