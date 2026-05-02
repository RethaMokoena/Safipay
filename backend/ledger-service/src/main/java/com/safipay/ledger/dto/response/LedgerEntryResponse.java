package com.safipay.ledger.dto.response;
import com.safipay.ledger.model.LedgerEntry;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;

@Data @Builder
public class LedgerEntryResponse {
    private String id; private String transactionRef;
    private String accountId; private LedgerEntry.EntryType entryType;
    private BigDecimal amount; private BigDecimal runningBalance;
    private LedgerEntry.EntryCategory category;
    private String description; private String externalRef;
    private String counterAccountId; private LocalDateTime createdAt;
}
