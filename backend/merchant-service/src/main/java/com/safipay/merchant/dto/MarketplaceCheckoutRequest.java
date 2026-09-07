package com.safipay.merchant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceCheckoutRequest {

    @NotEmpty(message = "Checkout must contain at least one item")
    @Valid
    private List<CheckoutItemRequest> items;
}