package com.safipay.stokvel.dto.response;
import com.safipay.stokvel.model.StokvelMember;
import lombok.*; import java.time.LocalDateTime;

@Data @Builder
public class MemberResponse {
    private Long id; private String userId; private StokvelMember.MemberStatus status;
    private Integer payoutOrder; private Boolean hasReceivedPayout; private LocalDateTime joinedAt;
}
