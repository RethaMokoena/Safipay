package com.safipay.merchant.dto.response;

import com.safipay.merchant.model.MerchantListing;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingResponse {

    private String id;

    /*
     * Merchant information is included so marketplace
     * results can show who is selling the item/service.
     */
    private String merchantId;

    private String merchantName;

    private String title;

    private String description;

    private BigDecimal price;

    private MerchantListing.ListingType type;

    private String imageUrl;

    private Integer stockQuantity;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}