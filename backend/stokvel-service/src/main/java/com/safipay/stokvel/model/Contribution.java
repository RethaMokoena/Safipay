package com.safipay.stokvel.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="contributions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contribution {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="stokvel_id",nullable=false) private Stokvel stokvel;
    @Column(nullable=false) private String userId;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ContributionStatus status;
    @Column private String transactionId;
    @Column(nullable=false) private Integer cycleNumber;
    @CreationTimestamp private LocalDateTime contributedAt;
    public enum ContributionStatus { PENDING, CONFIRMED, FAILED }
}
