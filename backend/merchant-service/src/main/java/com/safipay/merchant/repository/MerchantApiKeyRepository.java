package com.safipay.merchant.repository;
import com.safipay.merchant.model.MerchantApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, String> {
    List<MerchantApiKey> findByMerchantIdAndActiveTrue(String merchantId);
    Optional<MerchantApiKey> findByKeyPrefix(String keyPrefix);
}
