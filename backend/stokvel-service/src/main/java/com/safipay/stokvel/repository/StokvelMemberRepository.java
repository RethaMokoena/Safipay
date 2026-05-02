package com.safipay.stokvel.repository;
import com.safipay.stokvel.model.StokvelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface StokvelMemberRepository extends JpaRepository<StokvelMember, Long> {
    List<StokvelMember> findByStokvelIdAndStatus(Long stokvelId, StokvelMember.MemberStatus status);
    Optional<StokvelMember> findByStokvelIdAndUserId(Long stokvelId, String userId);
    boolean existsByStokvelIdAndUserId(Long stokvelId, String userId);
    long countByStokvelIdAndStatus(Long stokvelId, StokvelMember.MemberStatus status);
    List<StokvelMember> findByStokvelIdOrderByPayoutOrder(Long stokvelId);
}
