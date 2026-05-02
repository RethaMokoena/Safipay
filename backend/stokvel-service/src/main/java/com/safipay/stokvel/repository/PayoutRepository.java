package com.safipay.stokvel.repository;
import com.safipay.stokvel.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    List<Payout> findByStokvelId(Long stokvelId);
    List<Payout> findByRecipientUserId(String userId);
    boolean existsByStokvelIdAndRecipientUserIdAndCycleNumber(Long stokvelId, String userId, Integer cycle);
}
