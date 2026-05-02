package com.safipay.fraud.repository;

import com.safipay.fraud.model.FraudEvaluation;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudEvaluationRepository extends JpaRepository<FraudEvaluation, String> {

    Page<FraudEvaluation> findByUserIdOrderByEvaluatedAtDesc(String userId, Pageable pageable);

    List<FraudEvaluation> findByTransactionRef(String transactionRef);

    @Query("SELECT COUNT(e) FROM FraudEvaluation e WHERE e.userId = :userId AND e.evaluatedAt >= :since")
    long countByUserIdSince(String userId, LocalDateTime since);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM FraudEvaluation e WHERE e.userId = :userId AND e.evaluatedAt >= :since AND e.decision != 'BLOCKED'")
    BigDecimal sumAmountByUserIdSince(String userId, LocalDateTime since);

    @Query("SELECT COUNT(e) FROM FraudEvaluation e WHERE e.userId = :userId AND e.decision = 'BLOCKED' AND e.evaluatedAt >= :since")
    long countBlockedByUserIdSince(String userId, LocalDateTime since);
}
