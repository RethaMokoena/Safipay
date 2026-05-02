package com.safipay.payment.repository;

import com.safipay.payment.model.Payment;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query("SELECT p FROM Payment p WHERE p.senderUserId = :userId OR p.recipientUserId = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findByUserId(String userId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.senderUserId = :userId AND p.type = 'REQUEST_MONEY' AND p.status = 'PENDING'")
    List<Payment> findPendingRequestsForUser(String userId);
}
