package com.safipay.merchant.repository;

import com.safipay.merchant.model.MarketplaceCheckout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceCheckoutRepository
        extends JpaRepository<MarketplaceCheckout, String> {

    List<MarketplaceCheckout>
        findByBuyerUserIdOrderByCreatedAtDesc(
            String buyerUserId
        );
}