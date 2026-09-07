package com.safipay.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceCheckoutResponse {

    private String checkoutId;

    private String status;

    private BigDecimal grandTotal;

    private LocalDateTime createdAt;

    private List<MerchantOrderReceipt> merchantOrders;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantOrderReceipt {

        private String orderId;

        private String merchantId;

        private String merchantName;

        private String status;

        private BigDecimal totalAmount;

        private String paymentId;
        
        private List<OrderItemReceipt> items;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemReceipt {

        private String listingId;

        private String title;

        private String type;

        private Integer quantity;

        private BigDecimal unitPrice;

        private BigDecimal subtotal;
    }
}