package com.safipay.ledger.dto.response;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;

@Data @Builder
public class AccountStatementResponse {
    private String accountId; private String ownerId;
    private String currency;
    private BigDecimal openingBalance; private BigDecimal closingBalance;
    private BigDecimal totalCredits; private BigDecimal totalDebits;
    private LocalDateTime from; private LocalDateTime to;
    private List<LedgerEntryResponse> entries;
}
