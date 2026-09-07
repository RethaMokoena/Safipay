package com.safipay.merchant.repository;

import com.safipay.merchant.model.MerchantOrder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantOrderRepository
        extends JpaRepository<MerchantOrder, String> {

    List<MerchantOrder>
        findByCheckoutIdOrderByCreatedAtAsc(
            String checkoutId
        );

    List<MerchantOrder>
        findByBuyerUserIdOrderByCreatedAtDesc(
            String buyerUserId
        );

    List<MerchantOrder>
        findByMerchantIdOrderByCreatedAtDesc(
            String merchantId
        );

    /*
     * Marketplace refunds use the payment ID stored on MerchantOrder
     * to link the MerchantPayment back to the customer marketplace order.
     *
     * Merchant ID is included in the lookup so a payment can never be
     * attached to an order owned by a different merchant.
     */
    Optional<MerchantOrder>
        findByPaymentIdAndMerchantId(
            String paymentId,
            String merchantId
        );
}
