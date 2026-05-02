package com.safipay.stokvel.repository;
import com.safipay.stokvel.model.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    List<Contribution> findByStokvelId(Long stokvelId);
    List<Contribution> findByStokvelIdAndUserId(Long stokvelId, String userId);
    boolean existsByStokvelIdAndUserIdAndCycleNumber(Long stokvelId, String userId, Integer cycle);
    long countByStokvelIdAndCycleNumberAndStatus(Long stokvelId, Integer cycle, Contribution.ContributionStatus status);
    @Query("SELECT COALESCE(SUM(c.amount),0) FROM Contribution c WHERE c.stokvel.id=:stokvelId AND c.status='CONFIRMED'")
    BigDecimal sumConfirmed(Long stokvelId);
}
