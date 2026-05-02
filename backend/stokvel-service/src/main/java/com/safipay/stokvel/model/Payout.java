package com.safipay.stokvel.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="payouts") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payout {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="stokvel_id",nullable=false) private Stokvel stokvel;
    @Column(nullable=false) private String recipientUserId;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PayoutStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PayoutType type;
    @Column private Integer cycleNumber;
    @CreationTimestamp private LocalDateTime processedAt;
    public enum PayoutStatus { PENDING, COMPLETED, FAILED }
    public enum PayoutType { ROSCA_ROTATION, POOL_WITHDRAWAL }
}
