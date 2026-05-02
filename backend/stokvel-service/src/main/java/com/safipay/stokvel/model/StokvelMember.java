package com.safipay.stokvel.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="stokvel_members",uniqueConstraints=@UniqueConstraint(columnNames={"stokvel_id","userId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StokvelMember {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="stokvel_id",nullable=false) private Stokvel stokvel;
    @Column(nullable=false) private String userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private MemberStatus status=MemberStatus.ACTIVE;
    @Column private Integer payoutOrder;
    @Column(nullable=false) @Builder.Default private Boolean hasReceivedPayout=false;
    @CreationTimestamp private LocalDateTime joinedAt;
    public enum MemberStatus { ACTIVE, INACTIVE, REMOVED }
}
