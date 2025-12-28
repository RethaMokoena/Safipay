package com.safipay.merchant.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletBalanceResponse {
    private BigDecimal balance;
}
