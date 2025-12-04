package com.safipay.fraud.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class FraudRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID walletId;
    private BigDecimal amount;
    private boolean flagged;

    private LocalDateTime timestamp = LocalDateTime.now();
}
