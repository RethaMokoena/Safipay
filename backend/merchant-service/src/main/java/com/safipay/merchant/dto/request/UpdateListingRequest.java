package com.safipay.merchant.dto.request;

import com.safipay.merchant.model.MerchantListing;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingRequest {

    @Size(
            min = 1,
            max = 150,
            message = "Listing title must be between 1 and 150 characters"
    )
    private String title;

    @Size(
            max = 1000,
            message = "Description cannot exceed 1000 characters"
    )
    private String description;

    @DecimalMin(
            value = "0.01",
            message = "Price must be greater than 0"
    )
    private BigDecimal price;

    private MerchantListing.ListingType type;

    private String imageUrl;

    @PositiveOrZero(
            message = "Stock quantity cannot be negative"
    )
    private Integer stockQuantity;

    /*
     * Lets the merchant temporarily hide a listing
     * without deleting it.
     */
    private Boolean active;
}