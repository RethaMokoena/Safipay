package com.safipay.ledger.dto.request;

import com.safipay.ledger.model.LedgerEntry;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PostTransactionRequest {
    @NotBlank  private String debitOwnerId;   // who gets debited
    @NotBlank  private String creditOwnerId;  // who gets credited
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotNull   private LedgerEntry.EntryCategory category;
    private String description;
    private String externalRef;   // payment ID, stokvel ID, etc.
    private String initiatedBy;
}
