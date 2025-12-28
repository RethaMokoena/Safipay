package com.safipay.wallet.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID walletId;
    private BigDecimal amount;
    private String type; // CREDIT / DEBIT / TRANSFER
    private String description;

    private long timestamp;
}
