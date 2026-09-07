package com.safipay.merchant.repository;

import com.safipay.merchant.model.MerchantOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantOrderItemRepository
        extends JpaRepository<MerchantOrderItem, String> {

    List<MerchantOrderItem> findByOrderIdOrderByCreatedAtAsc(
        String orderId
    );

    List<MerchantOrderItem> findByOrderCheckoutId(
        String checkoutId
    );
}   