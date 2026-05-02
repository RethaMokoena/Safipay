package com.safipay.ledger.dto.request;

import com.safipay.ledger.model.LedgerAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotBlank private String ownerId;
    @NotNull  private LedgerAccount.AccountType type;
    private String description;
}
