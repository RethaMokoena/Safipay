package com.safipay.payment.model;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String senderUserId;
    @Column(nullable = false) private String recipientUserId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(length = 3) @Builder.Default private String currency = "ZAR";
    @Column private String description;
    @Column private String referenceNote;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentType type;

    @Column private String walletTransactionId;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public enum PaymentStatus { PENDING, COMPLETED, FAILED, REVERSED }
    public enum PaymentType { SEND_MONEY, REQUEST_MONEY, STOKVEL_CONTRIBUTION, STOKVEL_PAYOUT }
}
