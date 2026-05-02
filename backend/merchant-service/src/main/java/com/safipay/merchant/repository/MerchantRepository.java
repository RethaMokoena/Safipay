package com.safipay.merchant.repository;
import com.safipay.merchant.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*; import java.util.List;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {
    List<Merchant> findByOwnerUserId(String ownerUserId);
    Optional<Merchant> findByBusinessRegistrationNumber(String regNumber);
    boolean existsByOwnerUserIdAndBusinessName(String ownerUserId, String businessName);
}
