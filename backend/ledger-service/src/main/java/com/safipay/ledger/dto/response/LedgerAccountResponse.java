package com.safipay.ledger.dto.response;
import com.safipay.ledger.model.LedgerAccount;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;

@Data @Builder
public class LedgerAccountResponse {
    private String id; private String ownerId;
    private LedgerAccount.AccountType type;
    private String currency; private BigDecimal balance;
    private LedgerAccount.AccountStatus status;
    private String description; private LocalDateTime createdAt;
}
