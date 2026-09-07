package com.safipay.merchant.dto.response;

import com.safipay.merchant.model.MerchantListing;
import com.safipay.merchant.model.MerchantOrder;
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
public class MerchantOrderManagementResponse {

    private String orderId;

    private String checkoutId;

    private String merchantId;

    private String merchantName;

    private MerchantOrder.OrderStatus status;

    private BigDecimal totalAmount;

    private String paymentId;

    private LocalDateTime createdAt;

    private List<OrderItem> items;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {

        private String listingId;

        private String title;

        private MerchantListing.ListingType type;

        private Integer quantity;

        private BigDecimal unitPrice;

        private BigDecimal subtotal;
    }
}