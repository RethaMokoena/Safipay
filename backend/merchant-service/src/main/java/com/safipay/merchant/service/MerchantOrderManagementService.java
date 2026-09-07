package com.safipay.merchant.service;

import com.safipay.merchant.dto.response.MerchantOrderManagementResponse;
import com.safipay.merchant.exception.MerchantException;
import com.safipay.merchant.model.Merchant;
import com.safipay.merchant.model.MerchantOrder;
import com.safipay.merchant.model.MerchantOrderItem;
import com.safipay.merchant.repository.MerchantOrderItemRepository;
import com.safipay.merchant.repository.MerchantOrderRepository;
import com.safipay.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MerchantOrderManagementService {

    private final MerchantRepository merchantRepository;

    private final MerchantOrderRepository orderRepository;

    private final MerchantOrderItemRepository orderItemRepository;


    @Transactional(readOnly = true)
    public List<MerchantOrderManagementResponse> getMerchantOrders(
            String merchantId,
            String ownerUserId
    ) {

        Merchant merchant =
                getOwnedMerchant(
                        merchantId,
                        ownerUserId
                );


        return orderRepository
                .findByMerchantIdOrderByCreatedAtDesc(
                        merchant.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public MerchantOrderManagementResponse getMerchantOrder(
            String merchantId,
            String orderId,
            String ownerUserId
    ) {

        Merchant merchant =
                getOwnedMerchant(
                        merchantId,
                        ownerUserId
                );


        MerchantOrder order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () ->
                                        new MerchantException(
                                                "Order not found: " +
                                                        orderId
                                        )
                        );


        if (
                !order
                        .getMerchant()
                        .getId()
                        .equals(merchant.getId())
        ) {

            throw new MerchantException(
                    "Order does not belong to this merchant"
            );
        }


        return toResponse(
                order
        );
    }


    public MerchantOrderManagementResponse updateStatus(
            String merchantId,
            String orderId,
            MerchantOrder.OrderStatus requestedStatus,
            String ownerUserId
    ) {

        Merchant merchant =
                getOwnedMerchant(
                        merchantId,
                        ownerUserId
                );


        MerchantOrder order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () ->
                                        new MerchantException(
                                                "Order not found: " +
                                                        orderId
                                        )
                        );


        if (
                !order
                        .getMerchant()
                        .getId()
                        .equals(merchant.getId())
        ) {

            throw new MerchantException(
                    "Order does not belong to this merchant"
            );
        }


        validateStatusTransition(
                order.getStatus(),
                requestedStatus
        );


        order.setStatus(
                requestedStatus
        );


        orderRepository.saveAndFlush(
                order
        );


        return toResponse(
                order
        );
    }


    private void validateStatusTransition(
            MerchantOrder.OrderStatus current,
            MerchantOrder.OrderStatus requested
    ) {

        if (
                current ==
                MerchantOrder.OrderStatus.PAID
                &&
                requested ==
                MerchantOrder.OrderStatus.PROCESSING
        ) {
            return;
        }


        if (
                current ==
                MerchantOrder.OrderStatus.PROCESSING
                &&
                requested ==
                MerchantOrder.OrderStatus.COMPLETED
        ) {
            return;
        }


        throw new MerchantException(
                "Invalid order status transition: "
                        + current
                        + " -> "
                        + requested
        );
    }


    private Merchant getOwnedMerchant(
            String merchantId,
            String ownerUserId
    ) {

        Merchant merchant =
                merchantRepository
                        .findById(merchantId)
                        .orElseThrow(
                                () ->
                                        new MerchantException(
                                                "Merchant not found: "
                                                        + merchantId
                                        )
                        );


        if (
                !merchant
                        .getOwnerUserId()
                        .equals(ownerUserId)
        ) {

            throw new MerchantException(
                    "You do not own this merchant account"
            );
        }


        return merchant;
    }


    private MerchantOrderManagementResponse toResponse(
            MerchantOrder order
    ) {

        List<MerchantOrderItem> items =
                orderItemRepository
                        .findByOrderIdOrderByCreatedAtAsc(
                                order.getId()
                        );


        List<
                MerchantOrderManagementResponse.OrderItem
        > responseItems =
                items.stream()
                        .map(
                                item ->
                                        MerchantOrderManagementResponse
                                                .OrderItem
                                                .builder()
                                                .listingId(
                                                        item
                                                                .getListing()
                                                                .getId()
                                                )
                                                .title(
                                                        item.getTitle()
                                                )
                                                .type(
                                                        item.getType()
                                                )
                                                .quantity(
                                                        item.getQuantity()
                                                )
                                                .unitPrice(
                                                        item.getUnitPrice()
                                                )
                                                .subtotal(
                                                        item.getSubtotal()
                                                )
                                                .build()
                        )
                        .toList();


        return MerchantOrderManagementResponse
                .builder()
                .orderId(
                        order.getId()
                )
                .checkoutId(
                        order
                                .getCheckout()
                                .getId()
                )
                .merchantId(
                        order
                                .getMerchant()
                                .getId()
                )
                .merchantName(
                        order
                                .getMerchant()
                                .getBusinessName()
                )
                .status(
                        order.getStatus()
                )
                .totalAmount(
                        order.getTotalAmount()
                )
                .paymentId(
                        order.getPaymentId()
                )
                .createdAt(
                        order.getCreatedAt()
                )
                .items(
                        responseItems
                )
                .build();
    }
}