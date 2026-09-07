package com.safipay.merchant.controller;

import com.safipay.merchant.dto.response.ApiResponse;
import com.safipay.merchant.dto.MarketplaceCheckoutRequest;
import com.safipay.merchant.dto.MarketplaceCheckoutResponse;
import com.safipay.merchant.service.MarketplaceCheckoutService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MarketplaceCheckoutController {

    private final MarketplaceCheckoutService
        checkoutService;


    @PostMapping(
    "/checkout/{checkoutId}/pay"
)
public ResponseEntity<
    ApiResponse<
        MarketplaceCheckoutResponse
    >
> payCheckout(
        @PathVariable
        String checkoutId,

        @AuthenticationPrincipal
        String buyerUserId
) {

    MarketplaceCheckoutResponse result =
        checkoutService.payCheckout(
            checkoutId,
            buyerUserId
        );


    return ResponseEntity.ok(
        ApiResponse.success(
            result
        )
    );
}


    @PostMapping(
        "/checkout/{checkoutId}/orders/{orderId}/retry-payment"
    )
    public ResponseEntity<
        ApiResponse<
            MarketplaceCheckoutResponse
        >
    > retryFailedMerchantOrder(
            @PathVariable
            String checkoutId,

            @PathVariable
            String orderId,

            @AuthenticationPrincipal
            String buyerUserId
    ) {

        MarketplaceCheckoutResponse result =
                checkoutService
                        .retryFailedMerchantOrder(
                                checkoutId,
                                orderId,
                                buyerUserId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        result
                )
        );
    }

    @PostMapping("/checkout")
    public ResponseEntity<
        ApiResponse<
            MarketplaceCheckoutResponse
        >
    > checkout(
        @AuthenticationPrincipal
        String buyerUserId,

        @Valid
        @RequestBody
        MarketplaceCheckoutRequest request
    ) {

        MarketplaceCheckoutResponse
            checkout =
                checkoutService
                    .createCheckout(
                        buyerUserId,
                        request
                    );


        return ResponseEntity.ok(
            ApiResponse.success(
                checkout
            )
        );
    }

    @GetMapping(
        "/checkout/{checkoutId}"
)
public ResponseEntity<
        ApiResponse<
                MarketplaceCheckoutResponse
        >
> getCheckout(
        @PathVariable
        String checkoutId,

        @AuthenticationPrincipal
        String buyerUserId
) {

    MarketplaceCheckoutResponse result =
            checkoutService.getCheckout(
                    checkoutId,
                    buyerUserId
            );

    return ResponseEntity.ok(
            ApiResponse.success(
                    result
            )
    );
}

@GetMapping("/checkouts/my")
public ResponseEntity<
        ApiResponse<
                List<MarketplaceCheckoutResponse>
        >
> getMyCheckouts(
        @AuthenticationPrincipal
        String buyerUserId
) {

    List<MarketplaceCheckoutResponse> checkouts =
            checkoutService.getMyCheckouts(
                    buyerUserId
            );

    return ResponseEntity.ok(
            ApiResponse.success(
                    checkouts
            )
    );
}
}