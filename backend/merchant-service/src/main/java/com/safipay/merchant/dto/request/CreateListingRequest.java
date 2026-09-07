package com.safipay.merchant.dto.request;

import com.safipay.merchant.model.MerchantListing;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateListingRequest {

    @NotBlank(message = "Listing title is required")
    @Size(
            max = 150,
            message = "Listing title cannot exceed 150 characters"
    )
    private String title;

    @Size(
            max = 1000,
            message = "Description cannot exceed 1000 characters"
    )
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(
            value = "0.01",
            message = "Price must be greater than 0"
    )
    private BigDecimal price;

    @NotNull(message = "Listing type is required")
    private MerchantListing.ListingType type;

    private String imageUrl;

    /*
     * PRODUCT:
     * number of units currently available.
     *
     * SERVICE:
     * can be null.
     */
    @PositiveOrZero(
            message = "Stock quantity cannot be negative"
    )
    private Integer stockQuantity;
}