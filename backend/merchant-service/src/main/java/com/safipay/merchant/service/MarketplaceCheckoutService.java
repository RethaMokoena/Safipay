package com.safipay.merchant.service;

import com.safipay.merchant.dto.CheckoutItemRequest;
import com.safipay.merchant.dto.MarketplaceCheckoutRequest;
import com.safipay.merchant.dto.MarketplaceCheckoutResponse;
import com.safipay.merchant.exception.MerchantException;
import com.safipay.merchant.dto.response.MerchantPaymentResponse;       
import com.safipay.merchant.model.MarketplaceCheckout;
import com.safipay.merchant.model.Merchant;
import com.safipay.merchant.model.MerchantListing;
import com.safipay.merchant.model.MerchantOrder;
import com.safipay.merchant.model.MerchantOrderItem;
import com.safipay.merchant.model.MerchantPayment;
import com.safipay.merchant.repository.MarketplaceCheckoutRepository;
import com.safipay.merchant.repository.MerchantListingRepository;
import com.safipay.merchant.repository.MerchantOrderItemRepository;
import com.safipay.merchant.repository.MerchantOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceCheckoutService {

    private final MerchantListingRepository listingRepository;

    private final MarketplaceCheckoutRepository checkoutRepository;

    private final MerchantOrderRepository orderRepository;

    private final MerchantOrderItemRepository orderItemRepository;

    private final MerchantService merchantService;
    @Transactional
    public MarketplaceCheckoutResponse createCheckout(
            String buyerUserId,
            MarketplaceCheckoutRequest request
    ) {

        if (buyerUserId == null
                || buyerUserId.isBlank()) {
            throw new MerchantException(
                    "Authenticated buyer is required"
            );
        }


        /*
         * Combine duplicate listing IDs.
         *
         * Example:
         *
         * Burger x1
         * Burger x2
         *
         * becomes:
         *
         * Burger x3
         */
        Map<String, Integer> requestedQuantities
                = new LinkedHashMap<>();

        for (CheckoutItemRequest item
                : request.getItems()) {

            requestedQuantities.merge(
                    item.getListingId(),
                    item.getQuantity(),
                    Integer::sum
            );
        }

        List<String> listingIds
                = new ArrayList<>(
                        requestedQuantities.keySet()
                );

        List<MerchantListing> listings
                = listingRepository.findAllById(
                        listingIds
                );

        Map<String, MerchantListing> listingMap
                = listings.stream()
                        .collect(
                                Collectors.toMap(
                                        MerchantListing::getId,
                                        Function.identity()
                                )
                        );


        /*
         * Make sure every requested listing
         * actually exists.
         */
        for (String listingId : listingIds) {

            if (!listingMap.containsKey(
                    listingId
            )) {

                throw new MerchantException(
                        "Listing not found: "
                        + listingId
                );
            }
        }


        /*
         * Group validated items by merchant.
         */
        Map<String, MerchantGroup> merchantGroups
                = new LinkedHashMap<>();

        BigDecimal grandTotal
                = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry
                : requestedQuantities.entrySet()) {

            MerchantListing listing
                    = listingMap.get(
                            entry.getKey()
                    );

            int quantity
                    = entry.getValue();

            validateListing(
                    listing,
                    quantity
            );

            BigDecimal subtotal
                    = listing
                            .getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            quantity
                                    )
                            );

            Merchant merchant
                    = listing.getMerchant();

            MerchantGroup group
                    = merchantGroups
                            .computeIfAbsent(
                                    merchant.getId(),
                                    ignored
                                    -> new MerchantGroup(
                                            merchant
                                    )
                            );

            group.items.add(
                    new ResolvedItem(
                            listing,
                            quantity,
                            subtotal
                    )
            );

            group.total
                    = group.total.add(
                            subtotal
                    );

            grandTotal
                    = grandTotal.add(
                            subtotal
                    );
        }


        /*
         * Parent SafiPay checkout.
         */
        MarketplaceCheckout checkout
                = MarketplaceCheckout.builder()
                        .buyerUserId(
                                buyerUserId
                        )
                        .grandTotal(
                                grandTotal
                        )
                        .status(
                                MarketplaceCheckout.CheckoutStatus.PENDING
                        )
                        .build();

        checkout = checkoutRepository.saveAndFlush(checkout);

        List<    MarketplaceCheckoutResponse.MerchantOrderReceipt> receipts
                = new ArrayList<>();


        /*
         * One MerchantOrder per merchant.
         */
        for (MerchantGroup group
                : merchantGroups.values()) {

            MerchantOrder order
                    = MerchantOrder.builder()
                            .checkout(
                                    checkout
                            )
                            .merchant(
                                    group.merchant
                            )
                            .buyerUserId(
                                    buyerUserId
                            )
                            .totalAmount(
                                    group.total
                            )
                            .status(
                                    MerchantOrder.OrderStatus.PENDING_PAYMENT
                            )
                            .build();

            order
                    = orderRepository.save(
                            order
                    );

            List<
                MarketplaceCheckoutResponse.OrderItemReceipt> itemReceipts
                    = new ArrayList<>();

            for (ResolvedItem resolved
                    : group.items) {

                MerchantListing listing
                        = resolved.listing;

                MerchantOrderItem orderItem
                        = MerchantOrderItem.builder()
                                .order(
                                        order
                                )
                                .listing(
                                        listing
                                )
                                /*
                         * Snapshot the title and price.
                                 */
                                .title(
                                        listing.getTitle()
                                )
                                .type(
                                        listing.getType()
                                )
                                .quantity(
                                        resolved.quantity
                                )
                                .unitPrice(
                                        listing.getPrice()
                                )
                                .subtotal(
                                        resolved.subtotal
                                )
                                .build();

                orderItemRepository.save(
                        orderItem
                );

                itemReceipts.add(
                        MarketplaceCheckoutResponse.OrderItemReceipt
                                .builder()
                                .listingId(
                                        listing.getId()
                                )
                                .title(
                                        listing.getTitle()
                                )
                                .type(
                                        listing
                                                .getType()
                                                .name()
                                )
                                .quantity(
                                        resolved.quantity
                                )
                                .unitPrice(
                                        listing.getPrice()
                                )
                                .subtotal(
                                        resolved.subtotal
                                )
                                .build()
                );
            }

            receipts.add(
                    MarketplaceCheckoutResponse.MerchantOrderReceipt
                            .builder()
                            .orderId(
                                    order.getId()
                            )
                            .merchantId(
                                    group.merchant
                                            .getId()
                            )
                            .merchantName(
                                    group.merchant
                                            .getBusinessName()
                            )
                            .status(
                                    order
                                            .getStatus()
                                            .name()
                            )
                            .totalAmount(
                                    group.total
                            )
                            .items(
                                    itemReceipts
                            )
                            .build()
            );
        }

        return MarketplaceCheckoutResponse
                .builder()
                .checkoutId(
                        checkout.getId()
                )
                .status(
                        checkout
                                .getStatus()
                                .name()
                )
                .grandTotal(
                        grandTotal
                )
                .createdAt(
                        checkout.getCreatedAt()
                )
                .merchantOrders(
                        receipts
                )
                .build();
    }

    private void validateListing(
            MerchantListing listing,
            int quantity
    ) {

        if (!Boolean.TRUE.equals(
                listing.getActive()
        )) {
            throw new MerchantException(
                    "Listing is unavailable: "
                    + listing.getTitle()
            );
        }

        Merchant merchant
                = listing.getMerchant();

        if (merchant.getStatus()
                != Merchant.MerchantStatus.ACTIVE) {

            throw new MerchantException(
                    "Merchant is not active: "
                    + merchant.getBusinessName()
            );
        }

        if (listing.getType()
                == MerchantListing.ListingType.SERVICE) {

            if (quantity != 1) {

                throw new MerchantException(
                        "Services can only be booked once per checkout: "
                        + listing.getTitle()
                );
            }

            return;
        }

        Integer stock
                = listing.getStockQuantity();

        if (stock == null
                || stock <= 0) {

            throw new MerchantException(
                    "Product is out of stock: "
                    + listing.getTitle()
            );
        }

        if (quantity > stock) {

            throw new MerchantException(
                    "Only "
                    + stock
                    + " available for "
                    + listing.getTitle()
            );
        }
    }

    private static class MerchantGroup {

        private final Merchant merchant;

        private final List<ResolvedItem> items
                = new ArrayList<>();

        private BigDecimal total
                = BigDecimal.ZERO;

        private MerchantGroup(
                Merchant merchant
        ) {

            this.merchant
                    = merchant;
        }
    }

    private record ResolvedItem(
            MerchantListing listing,
            int quantity,
            BigDecimal subtotal
            ) {

    }


    @Transactional
public MarketplaceCheckoutResponse payCheckout(
        String checkoutId,
        String buyerUserId
) {

    MarketplaceCheckout checkout =
        checkoutRepository
            .findById(checkoutId)
            .orElseThrow(
                () ->
                    new MerchantException(
                        "Checkout not found: " +
                        checkoutId
                    )
            );


    if (
        !checkout
            .getBuyerUserId()
            .equals(buyerUserId)
    ) {
        throw new MerchantException(
            "You do not own this checkout"
        );
    }


    if (
        checkout.getStatus() ==
        MarketplaceCheckout.CheckoutStatus.PAID
    ) {
        return buildCheckoutResponse(
            checkout
        );
    }


    if (
        checkout.getStatus() ==
        MarketplaceCheckout.CheckoutStatus.CANCELLED
    ) {
        throw new MerchantException(
            "Checkout has been cancelled"
        );
    }


    checkout.setStatus(
        MarketplaceCheckout
            .CheckoutStatus
            .PROCESSING
    );

    checkoutRepository.save(
        checkout
    );


    List<MerchantOrder> orders =
        orderRepository
            .findByCheckoutIdOrderByCreatedAtAsc(
                checkoutId
            );


    int paidCount = 0;
    int failedCount = 0;


    for (
        MerchantOrder order :
            orders
    ) {

        if (
            order.getStatus() ==
            MerchantOrder.OrderStatus.PAID
        ) {

            paidCount++;
            continue;
        }


        if (
            order.getStatus() ==
            MerchantOrder
                .OrderStatus
                .PAYMENT_FAILED
        ) {

            failedCount++;
            continue;
        }


        try {

            List<MerchantOrderItem>
                orderItems =
                    orderItemRepository
                        .findByOrderIdOrderByCreatedAtAsc(
                            order.getId()
                        );


            /*
             * Validate stock again immediately
             * before taking payment.
             */
            for (
                MerchantOrderItem item :
                    orderItems
            ) {

                validateListing(
                    item.getListing(),
                    item.getQuantity()
                );
            }


            MerchantPaymentResponse payment =
                merchantService
                    .chargeMarketplaceOrder(
                        order,
                        buyerUserId
                    );


            order.setPaymentId(
                payment.getId()
            );


            if (
                payment.getStatus() ==
                MerchantPayment
                    .PaymentStatus
                    .COMPLETED
            ) {

                /*
                 * Payment succeeded.
                 * Now reduce product stock.
                 */
                for (
                    MerchantOrderItem item :
                        orderItems
                ) {

                    MerchantListing listing =
                        item.getListing();


                    if (
                        listing.getType() ==
                        MerchantListing
                            .ListingType
                            .PRODUCT
                    ) {

                        listing.setStockQuantity(
                            listing
                                .getStockQuantity()
                                -
                                item.getQuantity()
                        );


                        listingRepository.save(
                            listing
                        );
                    }
                }


                order.setStatus(
                    MerchantOrder
                        .OrderStatus
                        .PAID
                );

                paidCount++;

            } else {

                order.setStatus(
                    MerchantOrder
                        .OrderStatus
                        .PAYMENT_FAILED
                );

                failedCount++;
            }


        } catch (Exception e) {

            order.setStatus(
                MerchantOrder
                    .OrderStatus
                    .PAYMENT_FAILED
            );

            failedCount++;
        }


        orderRepository.save(
            order
        );
    }


    if (
        paidCount ==
        orders.size()
    ) {

        checkout.setStatus(
            MarketplaceCheckout
                .CheckoutStatus
                .PAID
        );

    } else if (
        paidCount > 0
    ) {

        checkout.setStatus(
            MarketplaceCheckout
                .CheckoutStatus
                .PARTIALLY_PAID
        );

    } else {

        checkout.setStatus(
            MarketplaceCheckout
                .CheckoutStatus
                .FAILED
        );
    }


    checkoutRepository
        .saveAndFlush(
            checkout
        );


    return buildCheckoutResponse(
        checkout
    );
}


    /**
     * Retry payment for exactly one failed merchant order.
     *
     * This deliberately does not re-run the whole checkout because
     * other merchant orders may already have been paid.
     */
    @Transactional
    public MarketplaceCheckoutResponse retryFailedMerchantOrder(
            String checkoutId,
            String orderId,
            String buyerUserId
    ) {

        MarketplaceCheckout checkout =
                checkoutRepository
                        .findById(checkoutId)
                        .orElseThrow(
                                () -> new MerchantException(
                                        "Checkout not found: "
                                        + checkoutId
                                )
                        );

        if (
                buyerUserId == null
                        || buyerUserId.isBlank()
        ) {
            throw new MerchantException(
                    "Authenticated buyer is required"
            );
        }

        if (
                !checkout
                        .getBuyerUserId()
                        .equals(buyerUserId)
        ) {
            throw new MerchantException(
                    "You do not own this checkout"
            );
        }

        if (
                checkout.getStatus()
                        == MarketplaceCheckout
                        .CheckoutStatus
                        .CANCELLED
        ) {
            throw new MerchantException(
                    "Checkout has been cancelled"
            );
        }

        MerchantOrder order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new MerchantException(
                                        "Merchant order not found: "
                                        + orderId
                                )
                        );

        if (
                order.getCheckout() == null
                        || !order.getCheckout()
                        .getId()
                        .equals(checkoutId)
        ) {
            throw new MerchantException(
                    "Merchant order does not belong to this checkout"
            );
        }

        if (
                !order.getBuyerUserId()
                        .equals(buyerUserId)
        ) {
            throw new MerchantException(
                    "You do not own this merchant order"
            );
        }

        /*
         * Critical duplicate-payment guard:
         * only an order that is still PAYMENT_FAILED may be retried.
         */
        if (
                order.getStatus()
                        != MerchantOrder
                        .OrderStatus
                        .PAYMENT_FAILED
        ) {
            throw new MerchantException(
                    "Only failed merchant payments can be retried"
            );
        }

        List<MerchantOrderItem> orderItems =
                orderItemRepository
                        .findByOrderIdOrderByCreatedAtAsc(
                                order.getId()
                        );

        /*
         * Re-check listing availability and product stock immediately
         * before retrying the charge.
         */
        for (
                MerchantOrderItem item :
                orderItems
        ) {
            validateListing(
                    item.getListing(),
                    item.getQuantity()
            );
        }

        MerchantPaymentResponse payment;

        try {

            payment =
                    merchantService
                            .chargeMarketplaceOrder(
                                    order,
                                    buyerUserId
                            );

        } catch (MerchantException e) {

            /*
             * The order is already PAYMENT_FAILED, so leave it in that
             * state and return the useful business error to the client.
             */
            throw e;

        } catch (Exception e) {

            throw new MerchantException(
                    "Could not retry this merchant payment"
            );
        }

        order.setPaymentId(
                payment.getId()
        );

        if (
                payment.getStatus()
                        == MerchantPayment
                        .PaymentStatus
                        .COMPLETED
        ) {

            /*
             * Stock was not reduced when the original payment failed.
             * Reduce it once, and only after this retry succeeds.
             */
            for (
                    MerchantOrderItem item :
                    orderItems
            ) {

                MerchantListing listing =
                        item.getListing();

                if (
                        listing.getType()
                                == MerchantListing
                                .ListingType
                                .PRODUCT
                ) {

                    Integer stock =
                            listing.getStockQuantity();

                    if (
                            stock == null
                                    || stock
                                    < item.getQuantity()
                    ) {
                        throw new MerchantException(
                                "Insufficient stock for "
                                + listing.getTitle()
                        );
                    }

                    listing.setStockQuantity(
                            stock
                                    - item.getQuantity()
                    );

                    listingRepository.save(
                            listing
                    );
                }
            }

            order.setStatus(
                    MerchantOrder
                            .OrderStatus
                            .PAID
            );

        } else {

            order.setStatus(
                    MerchantOrder
                            .OrderStatus
                            .PAYMENT_FAILED
            );
        }

        orderRepository.saveAndFlush(
                order
        );

        updateCheckoutPaymentStatus(
                checkout
        );

        return buildCheckoutResponse(
                checkout
        );
    }


    /**
     * Recalculate the parent checkout from its merchant-order payment
     * states after a single-order retry.
     */
    private void updateCheckoutPaymentStatus(
            MarketplaceCheckout checkout
    ) {

        List<MerchantOrder> orders =
                orderRepository
                        .findByCheckoutIdOrderByCreatedAtAsc(
                                checkout.getId()
                        );

        if (orders.isEmpty()) {
            checkout.setStatus(
                    MarketplaceCheckout
                            .CheckoutStatus
                            .FAILED
            );

            checkoutRepository.saveAndFlush(
                    checkout
            );

            return;
        }

        long successfulPayments =
                orders.stream()
                        .filter(
                                order ->
                                        isSuccessfullyPaidOrderStatus(
                                                order.getStatus()
                                        )
                        )
                        .count();

        long failedPayments =
                orders.stream()
                        .filter(
                                order ->
                                        order.getStatus()
                                                == MerchantOrder
                                                .OrderStatus
                                                .PAYMENT_FAILED
                        )
                        .count();

        if (
                successfulPayments
                        == orders.size()
        ) {

            checkout.setStatus(
                    MarketplaceCheckout
                            .CheckoutStatus
                            .PAID
            );

        } else if (
                successfulPayments > 0
        ) {

            checkout.setStatus(
                    MarketplaceCheckout
                            .CheckoutStatus
                            .PARTIALLY_PAID
            );

        } else if (
                failedPayments
                        == orders.size()
        ) {

            checkout.setStatus(
                    MarketplaceCheckout
                            .CheckoutStatus
                            .FAILED
            );

        } else {

            checkout.setStatus(
                    MarketplaceCheckout
                            .CheckoutStatus
                            .PROCESSING
            );
        }

        checkoutRepository.saveAndFlush(
                checkout
        );
    }


    /**
     * A merchant order can move into fulfilment after payment, so
     * PROCESSING and COMPLETED still represent a successful payment
     * when recalculating the checkout.
     */
    private boolean isSuccessfullyPaidOrderStatus(
            MerchantOrder.OrderStatus status
    ) {

        return status
                == MerchantOrder.OrderStatus.PAID
                || status
                == MerchantOrder.OrderStatus.PROCESSING
                || status
                == MerchantOrder.OrderStatus.COMPLETED;
    }


private MarketplaceCheckoutResponse buildCheckoutResponse(
        MarketplaceCheckout checkout
) {

    List<MerchantOrder> orders =
        orderRepository
            .findByCheckoutIdOrderByCreatedAtAsc(
                checkout.getId()
            );


    List<
        MarketplaceCheckoutResponse
            .MerchantOrderReceipt
    > receipts =
        new ArrayList<>();


    for (
        MerchantOrder order :
            orders
    ) {

        List<MerchantOrderItem> items =
            orderItemRepository
                .findByOrderIdOrderByCreatedAtAsc(
                    order.getId()
                );


        List<
            MarketplaceCheckoutResponse
                .OrderItemReceipt
        > itemReceipts =
            items.stream()
                .map(
                    item ->
                        MarketplaceCheckoutResponse
                            .OrderItemReceipt
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
                                item
                                    .getType()
                                    .name()
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


        receipts.add(
            MarketplaceCheckoutResponse
                .MerchantOrderReceipt
                .builder()
                .orderId(
                    order.getId()
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
                .paymentId(
                    order.getPaymentId()
                )
                .status(
                    order
                        .getStatus()
                        .name()
                )
                .totalAmount(
                    order.getTotalAmount()
                )
                .items(
                    itemReceipts
                )
                .build()
        );
    }


    return MarketplaceCheckoutResponse
        .builder()
        .checkoutId(
            checkout.getId()
        )
        .status(
            checkout
                .getStatus()
                .name()
        )
        .grandTotal(
            checkout.getGrandTotal()
        )
        .createdAt(
            checkout.getCreatedAt()
        )
        .merchantOrders(
            receipts
        )
        .build();
}


@Transactional(readOnly = true)
public MarketplaceCheckoutResponse getCheckout(
        String checkoutId,
        String buyerUserId
) {

    MarketplaceCheckout checkout =
            checkoutRepository
                    .findById(checkoutId)
                    .orElseThrow(
                            () -> new MerchantException(
                                    "Checkout not found: " +
                                    checkoutId
                            )
                    );

    if (
            !checkout
                    .getBuyerUserId()
                    .equals(buyerUserId)
    ) {
        throw new MerchantException(
                "You do not own this checkout"
        );
    }

    return buildCheckoutResponse(
            checkout
    );
}

@Transactional(readOnly = true)
public List<MarketplaceCheckoutResponse> getMyCheckouts(
        String buyerUserId
) {

    return checkoutRepository
            .findByBuyerUserIdOrderByCreatedAtDesc(
                    buyerUserId
            )
            .stream()
            .map(this::buildCheckoutResponse)
            .toList();
}
}
