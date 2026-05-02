package com.safipay.merchant.repository;
import com.safipay.merchant.model.MerchantPayment;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantPaymentRepository extends JpaRepository<MerchantPayment, String> {
    Page<MerchantPayment> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);
    Page<MerchantPayment> findByPayerUserIdOrderByCreatedAtDesc(String payerUserId, Pageable pageable);
    boolean existsByMerchantReferenceAndMerchantId(String merchantReference, String merchantId);
}
