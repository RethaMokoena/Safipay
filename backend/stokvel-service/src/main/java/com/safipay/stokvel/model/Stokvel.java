package com.safipay.stokvel.model;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import lombok.*;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name="stokvels") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Stokvel {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) private String name;
    @Column(length=500) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private StokvelType type;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private StokvelStatus status = StokvelStatus.FORMING;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal contributionAmount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ContributionFrequency contributionFrequency;
    @Column(nullable=false) private Integer maxMembers;
    @Column(nullable=false) private String adminUserId;
    @Column(nullable=false,precision=19,scale=2) @Builder.Default private BigDecimal totalPoolBalance=BigDecimal.ZERO;
    @Column @Builder.Default private Integer currentPayoutIndex=0;
    @OneToMany(mappedBy="stokvel",cascade=CascadeType.ALL,orphanRemoval=true) @Builder.Default private List<StokvelMember> members=new ArrayList<>();
    @OneToMany(mappedBy="stokvel",cascade=CascadeType.ALL,orphanRemoval=true) @Builder.Default private List<Contribution> contributions=new ArrayList<>();
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public enum StokvelType { ROSCA, POOL }
    public enum StokvelStatus { FORMING, ACTIVE, COMPLETED, SUSPENDED }
    public enum ContributionFrequency { WEEKLY, BIWEEKLY, MONTHLY }
}
