package com.safipay.fraud.repository;

import com.safipay.fraud.model.FraudAlert;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, String> {
    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlert.AlertStatus status, Pageable pageable);
    List<FraudAlert> findByUserId(String userId);
    long countByUserIdAndStatus(String userId, FraudAlert.AlertStatus status);
}
