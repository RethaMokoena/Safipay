package com.safipay.paymentgateway.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String idempotencyKey;

    private UUID payerWalletId;
    private UUID merchantWalletId;

    private BigDecimal amount;
    private String status; // PENDING/SUCCESS/FAILED/COMPENSATION_REQUIRED
    private String message;

    private Instant createdAt;
}
