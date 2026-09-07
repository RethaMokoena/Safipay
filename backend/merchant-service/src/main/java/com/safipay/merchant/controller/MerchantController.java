package com.safipay.merchant.controller;

import com.safipay.merchant.dto.request.*;
import com.safipay.merchant.dto.response.*;
import com.safipay.merchant.model.Merchant;
import com.safipay.merchant.model.MerchantListing;
import com.safipay.merchant.service.MerchantService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;


    // =========================================================
    // MERCHANT REGISTRATION / CRUD
    // =========================================================

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> register(
            @Valid @RequestBody RegisterMerchantRequest req,
            @AuthenticationPrincipal String userId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Merchant registered, pending verification",
                                merchantService.registerMerchant(
                                        req,
                                        userId
                                )
                        )
                );
    }


    /**
     * Returns merchants owned by the logged-in user.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> getMyMerchants(
            @AuthenticationPrincipal String userId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.getMyMerchants(
                                userId
                        )
                )
        );
    }

    // =========================================================
// GLOBAL MARKETPLACE
// =========================================================

@GetMapping("/listings")
public ResponseEntity<ApiResponse<List<ListingResponse>>> discoverListings(

        @RequestParam(required = false)
        String q,

        @RequestParam(required = false)
        Merchant.BusinessCategory category,

        @RequestParam(required = false)
        MerchantListing.ListingType type,

        @RequestParam(required = false)
        BigDecimal maxPrice
) {

    return ResponseEntity.ok(
            ApiResponse.success(
                    merchantService.discoverListings(
                            q,
                            category,
                            type,
                            maxPrice
                    )
            )
    );
}
    // =========================================================
    // MERCHANT DISCOVERY
    // =========================================================

    /**
     * Public merchant discovery.
     *
     * Examples:
     *
     * GET /api/merchants/discover
     *
     * GET /api/merchants/discover?category=FOOD_BEVERAGE
     *
     * GET /api/merchants/discover?q=food
     */
    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> discoverMerchants(
            @RequestParam(required = false)
            Merchant.BusinessCategory category,

            @RequestParam(required = false)
            String q
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.discoverMerchants(
                                category,
                                q
                        )
                )
        );
    }


    /**
     * Returns one merchant.
     */
    @GetMapping("/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchant(
            @PathVariable String merchantId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.getMerchant(
                                merchantId
                        )
                )
        );
    }


    // =========================================================
    // ADMIN MERCHANT STATUS
    // =========================================================

    @PostMapping("/{merchantId}/approve")
    public ResponseEntity<ApiResponse<MerchantResponse>> approve(
            @PathVariable String merchantId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Merchant approved",
                        merchantService.approveMerchant(
                                merchantId
                        )
                )
        );
    }


    @PostMapping("/{merchantId}/suspend")
    public ResponseEntity<ApiResponse<MerchantResponse>> suspend(
            @PathVariable String merchantId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Merchant suspended",
                        merchantService.suspendMerchant(
                                merchantId
                        )
                )
        );
    }


    // =========================================================
    // MARKETPLACE / MERCHANT LISTINGS
    // =========================================================

    /**
     * Merchant owner creates a new product or service listing.
     *
     * POST
     * /api/merchants/{merchantId}/listings
     */
    @PostMapping("/{merchantId}/listings")
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @PathVariable String merchantId,

            @Valid
            @RequestBody
            CreateListingRequest req,

            @AuthenticationPrincipal
            String userId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Listing created",
                                merchantService.createListing(
                                        merchantId,
                                        req,
                                        userId
                                )
                        )
                );
    }


    /**
     * Merchant dashboard.
     *
     * Returns all listings owned by this merchant,
     * including disabled listings.
     *
     * GET
     * /api/merchants/{merchantId}/listings/manage
     */
    @GetMapping("/{merchantId}/listings/manage")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getMerchantListings(
            @PathVariable String merchantId,

            @AuthenticationPrincipal
            String userId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.getMerchantListings(
                                merchantId,
                                userId
                        )
                )
        );
    }


    /**
     * Public merchant storefront.
     *
     * Customers only receive ACTIVE listings
     * belonging to an ACTIVE merchant.
     *
     * GET
     * /api/merchants/{merchantId}/listings
     */
    @GetMapping("/{merchantId}/listings")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getPublicMerchantListings(
            @PathVariable String merchantId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.getPublicMerchantListings(
                                merchantId
                        )
                )
        );
    }


    /**
     * Public listing detail.
     *
     * GET
     * /api/merchants/{merchantId}/listings/{listingId}
     */
    @GetMapping("/{merchantId}/listings/{listingId}")
    public ResponseEntity<ApiResponse<ListingResponse>> getPublicListing(
            @PathVariable String merchantId,
            @PathVariable String listingId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.getPublicListing(
                                merchantId,
                                listingId
                        )
                )
        );
    }


    /**
     * Merchant owner updates a listing.
     *
     * PUT
     * /api/merchants/{merchantId}/listings/{listingId}
     */
    @PutMapping("/{merchantId}/listings/{listingId}")
    public ResponseEntity<ApiResponse<ListingResponse>> updateListing(
            @PathVariable String merchantId,

            @PathVariable String listingId,

            @Valid
            @RequestBody
            UpdateListingRequest req,

            @AuthenticationPrincipal
            String userId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing updated",
                        merchantService.updateListing(
                                merchantId,
                                listingId,
                                req,
                                userId
                        )
                )
        );
    }


    /**
     * Merchant owner disables a listing.
     *
     * This performs a soft delete.
     *
     * DELETE
     * /api/merchants/{merchantId}/listings/{listingId}
     */
    @DeleteMapping("/{merchantId}/listings/{listingId}")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @PathVariable String merchantId,

            @PathVariable String listingId,

            @AuthenticationPrincipal
            String userId
    ) {

        merchantService.deleteListing(
                merchantId,
                listingId,
                userId
        );


        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing removed",
                        null
                )
        );
    }


    // =========================================================
    // API KEYS
    // =========================================================

    @PostMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> generateKey(
            @PathVariable String merchantId,

            @Valid
            @RequestBody
            CreateApiKeyRequest req,

            @AuthenticationPrincipal
            String userId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "API key created — save it now, it won't be shown again",
                                merchantService.generateApiKey(
                                        merchantId,
                                        req,
                                        userId
                                )
                        )
                );
    }


    @GetMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listKeys(
            @PathVariable String merchantId,

            @AuthenticationPrincipal
            String userId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.listApiKeys(
                                merchantId,
                                userId
                        )
                )
        );
    }


    @DeleteMapping("/{merchantId}/api-keys/{keyId}")
    public ResponseEntity<ApiResponse<Void>> revokeKey(
            @PathVariable String merchantId,

            @PathVariable String keyId,

            @AuthenticationPrincipal
            String userId
    ) {

        merchantService.revokeApiKey(
                keyId,
                merchantId,
                userId
        );


        return ResponseEntity.ok(
                ApiResponse.success(
                        "API key revoked",
                        null
                )
        );
    }


    // =========================================================
    // MERCHANT PAYMENTS
    // =========================================================

    @PostMapping("/{merchantId}/payments/charge")
    public ResponseEntity<ApiResponse<MerchantPaymentResponse>> charge(
            @PathVariable String merchantId,

            @Valid
            @RequestBody
            MerchantPaymentRequest req,

            @AuthenticationPrincipal
            String userId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Payment successful",
                                merchantService.chargeCustomer(
                                        merchantId,
                                        req,
                                        userId
                                )
                        )
                );
    }


    @PostMapping("/{merchantId}/payments/{paymentId}/refund")
    public ResponseEntity<ApiResponse<MerchantPaymentResponse>> refund(
            @PathVariable String merchantId,

            @PathVariable String paymentId,

            @AuthenticationPrincipal
            String userId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refund processed",
                        merchantService.refundPayment(
                                paymentId,
                                merchantId,
                                userId
                        )
                )
        );
    }


    @GetMapping("/{merchantId}/payments")
    public ResponseEntity<ApiResponse<List<MerchantPaymentResponse>>> getPayments(
            @PathVariable String merchantId,

            @AuthenticationPrincipal
            String userId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        merchantService.getPayments(
                                merchantId,
                                userId,
                                page,
                                size
                        )
                )
        );
    }
}