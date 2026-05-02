package com.safipay.stokvel.dto.response;
import com.safipay.stokvel.model.Stokvel;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;

@Data @Builder
public class StokvelResponse {
    private Long id; private String name; private String description;
    private Stokvel.StokvelType type; private Stokvel.StokvelStatus status;
    private BigDecimal contributionAmount; private Stokvel.ContributionFrequency contributionFrequency;
    private Integer maxMembers; private Integer currentMemberCount;
    private String adminUserId; private BigDecimal totalPoolBalance;
    private Integer currentPayoutIndex; private List<MemberResponse> members;
    private LocalDateTime createdAt;
}
