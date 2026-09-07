package com.safipay.merchant.service;

import com.safipay.merchant.dto.request.*;
import com.safipay.merchant.dto.response.*;
import com.safipay.merchant.exception.MerchantException;
import org.springframework.transaction.annotation.Propagation;
import com.safipay.merchant.model.*;
import com.safipay.merchant.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MerchantService {

    private final MerchantRepository merchantRepo;
    private final MerchantApiKeyRepository apiKeyRepo;
    private final MerchantPaymentRepository paymentRepo;
    private final MerchantListingRepository listingRepo;
    private final MerchantOrderRepository merchantOrderRepo;

    private final PasswordEncoder passwordEncoder;

    private final RestTemplate restTemplate = new RestTemplate();


    @Value("${services.wallet-service-url}")
    private String walletServiceUrl;


    private static final BigDecimal FEE_RATE =
            new BigDecimal("0.015");

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();


    // =========================================================
    // MERCHANT REGISTRATION
    // =========================================================

    public MerchantResponse registerMerchant(
            RegisterMerchantRequest req,
            String ownerUserId
    ) {

        if (
                merchantRepo.existsByOwnerUserIdAndBusinessName(
                        ownerUserId,
                        req.getBusinessName()
                )
        ) {

            throw new MerchantException(
                    "You already have a merchant account with this business name"
            );
        }


        Merchant merchant =
                merchantRepo.save(
                        Merchant.builder()
                                .ownerUserId(ownerUserId)
                                .businessName(req.getBusinessName())
                                .businessRegistrationNumber(
                                        req.getBusinessRegistrationNumber()
                                )
                                .category(req.getCategory())
                                .businessEmail(req.getBusinessEmail())
                                .businessPhone(req.getBusinessPhone())
                                .description(req.getDescription())
                                .build()
                );


        // =====================================================
        // AUTO-CREATE MERCHANT WALLET
        // =====================================================

        try {

            if (
                    req.getBusinessEmail() == null
                            || req.getBusinessEmail().isBlank()
            ) {

                throw new MerchantException(
                        "Business email is required to create a merchant wallet"
                );
            }


            Map<String, String> requestBody =
                    Map.of(
                            "email",
                            req.getBusinessEmail()
                    );


            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            walletServiceUrl
                                    + "/internal/wallets/"
                                    + merchant.getId()
                                    + "/create",
                            requestBody,
                            Map.class
                    );


            Map responseBody =
                    response.getBody();


            if (
                    responseBody == null
                            || responseBody.get("id") == null
            ) {

                throw new MerchantException(
                        "Wallet service did not return a wallet ID"
                );
            }


            String walletId =
                    responseBody
                            .get("id")
                            .toString();


            merchant.setWalletId(walletId);

            merchantRepo.save(merchant);


            log.info(
                    "Created merchant wallet {} for merchant {}",
                    walletId,
                    merchant.getId()
            );

        } catch (Exception e) {

            log.warn(
                    "Could not auto-create merchant wallet for merchant {}: {}",
                    merchant.getId(),
                    e.getMessage()
            );
        }


        log.info(
                "Registered merchant '{}' (id={}) for user {}",
                merchant.getBusinessName(),
                merchant.getId(),
                ownerUserId
        );


        return toMerchantResponse(merchant);
    }


    // =========================================================
    // MERCHANT APPROVAL / STATUS
    // =========================================================

    public MerchantResponse approveMerchant(
            String merchantId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        merchant.setStatus(
                Merchant.MerchantStatus.ACTIVE
        );


        log.info(
                "Approved merchant {}",
                merchantId
        );


        return toMerchantResponse(
                merchantRepo.save(merchant)
        );
    }


    public MerchantResponse suspendMerchant(
            String merchantId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        merchant.setStatus(
                Merchant.MerchantStatus.SUSPENDED
        );


        log.warn(
                "Suspended merchant {}",
                merchantId
        );


        return toMerchantResponse(
                merchantRepo.save(merchant)
        );
    }


    // =========================================================
    // MERCHANT LOOKUPS
    // =========================================================

    @Transactional(readOnly = true)
    public List<MerchantResponse> getMyMerchants(
            String ownerUserId
    ) {

        return merchantRepo
                .findByOwnerUserId(ownerUserId)
                .stream()
                .map(this::toMerchantResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(
            String merchantId
    ) {

        return toMerchantResponse(
                getMerchantOrThrow(merchantId)
        );
    }


    // =========================================================
    // MERCHANT DISCOVERY
    // =========================================================

    @Transactional(readOnly = true)
    public List<MerchantResponse> discoverMerchants(
            Merchant.BusinessCategory category,
            String q
    ) {

        String query =
                q == null
                        ? ""
                        : q.trim().toLowerCase();


        String categoryQuery =
                query.replace(
                        " ",
                        "_"
                );


        return merchantRepo
                .findByStatus(
                        Merchant.MerchantStatus.ACTIVE
                )
                .stream()

                .filter(
                        merchant ->
                                category == null
                                        || merchant.getCategory()
                                        == category
                )

                .filter(merchant -> {

                    if (query.isBlank()) {
                        return true;
                    }


                    String name =
                            merchant.getBusinessName() == null
                                    ? ""
                                    : merchant
                                            .getBusinessName()
                                            .toLowerCase();


                    String description =
                            merchant.getDescription() == null
                                    ? ""
                                    : merchant
                                            .getDescription()
                                            .toLowerCase();


                    String merchantCategory =
                            merchant.getCategory() == null
                                    ? ""
                                    : merchant
                                            .getCategory()
                                            .name()
                                            .toLowerCase();


                    return name.contains(query)
                            || description.contains(query)
                            || merchantCategory.contains(
                                    categoryQuery
                            );
                })

                .map(this::toMerchantResponse)

                .collect(Collectors.toList());
    }


    // =========================================================
    // MERCHANT LISTINGS / MARKETPLACE
    // =========================================================

    /**
     * Create a new product/service listing.
     *
     * Only the owner of an ACTIVE merchant can create listings.
     */
    public ListingResponse createListing(
            String merchantId,
            CreateListingRequest req,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        if (
                merchant.getStatus()
                        != Merchant.MerchantStatus.ACTIVE
        ) {

            throw new MerchantException(
                    "Merchant account must be ACTIVE to create listings"
            );
        }


        // Products must have stock.
        if (
                req.getType()
                        == MerchantListing.ListingType.PRODUCT
                        && req.getStockQuantity() == null
        ) {

            throw new MerchantException(
                    "Stock quantity is required for product listings"
            );
        }


        // Services do not use stock.
        if (
                req.getType()
                        == MerchantListing.ListingType.SERVICE
                        && req.getStockQuantity() != null
        ) {

            throw new MerchantException(
                    "Stock quantity does not apply to service listings"
            );
        }


        String imageUrl = null;

        if (
                req.getImageUrl() != null
                        && !req.getImageUrl().isBlank()
        ) {

            imageUrl =
                    req.getImageUrl().trim();
        }


        MerchantListing listing =
                MerchantListing.builder()

                        .merchant(merchant)

                        .title(
                                req.getTitle().trim()
                        )

                        .description(
                                req.getDescription() == null
                                        ? null
                                        : req.getDescription().trim()
                        )

                        .price(
                                req.getPrice()
                        )

                        .type(
                                req.getType()
                        )

                        .imageUrl(
                                imageUrl
                        )

                        .stockQuantity(
                                req.getType()
                                        == MerchantListing.ListingType.PRODUCT
                                        ? req.getStockQuantity()
                                        : null
                        )

                        .active(true)

                        .build();


        listing =
                listingRepo.save(listing);


        log.info(
                "Created listing '{}' ({}) for merchant {}",
                listing.getTitle(),
                listing.getId(),
                merchantId
        );


        return toListingResponse(listing);
    }


    /**
     * Merchant dashboard listing view.
     *
     * Includes active and inactive listings.
     */
    @Transactional(readOnly = true)
    public List<ListingResponse> getMerchantListings(
            String merchantId,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        return listingRepo
                .findByMerchantId(merchantId)
                .stream()
                .map(this::toListingResponse)
                .collect(Collectors.toList());
    }

 @Transactional(readOnly = true)
public List<ListingResponse> discoverListings(
        String q,
        Merchant.BusinessCategory category,
        MerchantListing.ListingType type,
        BigDecimal maxPrice
) {

    String query =
            q == null
                    ? ""
                    : q.trim();


    if (
            maxPrice != null
                    && maxPrice.compareTo(BigDecimal.ZERO) < 0
    ) {

        throw new MerchantException(
                "Maximum price cannot be negative"
        );
    }


    return listingRepo
            .discoverListings(
                    query,
                    category,
                    type,
                    maxPrice,
                    Merchant.MerchantStatus.ACTIVE
            )
            .stream()
            .map(this::toListingResponse)
            .collect(Collectors.toList());
}


@Transactional(
    propagation = Propagation.REQUIRES_NEW
)
public MerchantPaymentResponse chargeMarketplaceOrder(
        MerchantOrder order,
        String buyerUserId
) {

    Merchant merchant =
        order.getMerchant();

    if (
        merchant.getStatus() !=
        Merchant.MerchantStatus.ACTIVE
    ) {
        throw new MerchantException(
            "Merchant account is not active"
        );
    }

    if (
        !order.getBuyerUserId()
            .equals(buyerUserId)
    ) {
        throw new MerchantException(
            "You do not own this order"
        );
    }

    if (
        order.getStatus() !=
        MerchantOrder.OrderStatus.PENDING_PAYMENT
    ) {
        throw new MerchantException(
            "Order is not awaiting payment"
        );
    }

    BigDecimal amount =
        order.getTotalAmount();

    String merchantReference =
        "marketplace-order:" +
        order.getId();


    /*
     * Prevent the same merchant order
     * from being paid twice.
     */
    if (
        paymentRepo
            .existsByMerchantReferenceAndMerchantId(
                merchantReference,
                merchant.getId()
            )
    ) {
        throw new MerchantException(
            "Marketplace order payment already processed"
        );
    }


    BigDecimal feeAmount =
        amount
            .multiply(FEE_RATE)
            .setScale(
                2,
                RoundingMode.HALF_UP
            );

    BigDecimal netAmount =
        amount.subtract(
            feeAmount
        );


    MerchantPayment payment =
        paymentRepo.save(
            MerchantPayment.builder()
                .merchant(
                    merchant
                )
                .payerUserId(
                    buyerUserId
                )
                .amount(
                    amount
                )
                .description(
                    "Marketplace order " +
                    order.getId()
                )
                .merchantReference(
                    merchantReference
                )
                .feeAmount(
                    feeAmount
                )
                .netAmount(
                    netAmount
                )
                .build()
        );


    boolean payerDebited =
        false;


    try {

        /*
         * Customer pays the full
         * merchant order amount.
         */
        debitWallet(
            buyerUserId,
            amount,
            payment.getId(),
            "Marketplace payment to " +
            merchant.getBusinessName()
        );

        payerDebited = true;


        /*
         * Merchant receives the
         * amount after SafiPay's fee.
         */
        creditWallet(
            merchant.getId(),
            netAmount,
            payment.getId(),
            "Marketplace payment from customer"
        );


        payment.setStatus(
            MerchantPayment.PaymentStatus.COMPLETED
        );

        payment.setPaymentTransactionId(
            payment.getId()
        );

    } catch (Exception e) {

        /*
         * If customer debit succeeded but
         * merchant credit failed, return
         * the money to the customer.
         */
        if (payerDebited) {

            try {

                creditWallet(
                    buyerUserId,
                    amount,
                    payment.getId() +
                    "-reversal",
                    "Marketplace payment reversal"
                );

            } catch (Exception reversalError) {

                log.error(
                    "Marketplace payment reversal failed for payment {}: {}",
                    payment.getId(),
                    reversalError.getMessage()
                );
            }
        }


        payment.setStatus(
            MerchantPayment.PaymentStatus.FAILED
        );

        paymentRepo.save(
            payment
        );

        return toPaymentResponse(
            payment
        );
    }


    paymentRepo.save(
        payment
    );


    log.info(
        "Marketplace payment {} completed: R{} from {} to {}",
        payment.getId(),
        amount,
        buyerUserId,
        merchant.getId()
    );


    return toPaymentResponse(
        payment
    );
}
    /**
     * Public storefront.
     *
     * Only ACTIVE merchants and active listings are visible.
     */
    @Transactional(readOnly = true)
    public List<ListingResponse> getPublicMerchantListings(
            String merchantId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        if (
                merchant.getStatus()
                        != Merchant.MerchantStatus.ACTIVE
        ) {

            throw new MerchantException(
                    "Merchant is not currently available"
            );
        }


        return listingRepo
                .findByMerchantIdAndActiveTrue(
                        merchantId
                )
                .stream()
                .map(this::toListingResponse)
                .collect(Collectors.toList());
    }


    /**
     * Get one public listing.
     */
    @Transactional(readOnly = true)
    public ListingResponse getPublicListing(
            String merchantId,
            String listingId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        if (
                merchant.getStatus()
                        != Merchant.MerchantStatus.ACTIVE
        ) {

            throw new MerchantException(
                    "Merchant is not currently available"
            );
        }


        MerchantListing listing =
                getListingOrThrow(
                        merchantId,
                        listingId
                );


        if (
                !Boolean.TRUE.equals(
                        listing.getActive()
                )
        ) {

            throw new MerchantException(
                    "Listing is not currently available"
            );
        }


        return toListingResponse(listing);
    }


    /**
     * Update an existing merchant listing.
     */
    public ListingResponse updateListing(
            String merchantId,
            String listingId,
            UpdateListingRequest req,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        MerchantListing listing =
                getListingOrThrow(
                        merchantId,
                        listingId
                );


        // =====================================================
        // TITLE
        // =====================================================

        if (req.getTitle() != null) {

            String title =
                    req.getTitle().trim();


            if (title.isBlank()) {

                throw new MerchantException(
                        "Listing title cannot be blank"
                );
            }


            listing.setTitle(title);
        }


        // =====================================================
        // DESCRIPTION
        // =====================================================

        if (req.getDescription() != null) {

            listing.setDescription(
                    req.getDescription().trim()
            );
        }


        // =====================================================
        // PRICE
        // =====================================================

        if (req.getPrice() != null) {

            listing.setPrice(
                    req.getPrice()
            );
        }


        // =====================================================
        // IMAGE
        // =====================================================

        if (req.getImageUrl() != null) {

            String imageUrl =
                    req.getImageUrl().trim();


            listing.setImageUrl(
                    imageUrl.isBlank()
                            ? null
                            : imageUrl
            );
        }


        // =====================================================
        // TYPE
        // =====================================================

        if (req.getType() != null) {

            listing.setType(
                    req.getType()
            );


            /*
             * Services don't have stock.
             */
            if (
                    req.getType()
                            == MerchantListing.ListingType.SERVICE
            ) {

                listing.setStockQuantity(null);
            }
        }


        // =====================================================
        // STOCK
        // =====================================================

        if (req.getStockQuantity() != null) {

            if (
                    listing.getType()
                            == MerchantListing.ListingType.SERVICE
            ) {

                throw new MerchantException(
                        "Stock quantity does not apply to service listings"
                );
            }


            listing.setStockQuantity(
                    req.getStockQuantity()
            );
        }


        /*
         * If the resulting listing is a PRODUCT,
         * stock must exist.
         */
        if (
                listing.getType()
                        == MerchantListing.ListingType.PRODUCT
                        && listing.getStockQuantity() == null
        ) {

            throw new MerchantException(
                    "Stock quantity is required for product listings"
            );
        }


        // =====================================================
        // ACTIVE STATUS
        // =====================================================

        if (req.getActive() != null) {

            /*
             * Suspended/pending merchants cannot publish.
             */
            if (
                    Boolean.TRUE.equals(
                            req.getActive()
                    )
                            && merchant.getStatus()
                            != Merchant.MerchantStatus.ACTIVE
            ) {

                throw new MerchantException(
                        "Merchant must be ACTIVE to publish listings"
                );
            }


            listing.setActive(
                    req.getActive()
            );
        }


        listing =
                listingRepo.save(listing);


        log.info(
                "Updated listing {} for merchant {}",
                listingId,
                merchantId
        );


        return toListingResponse(listing);
    }


    /**
     * Soft delete.
     *
     * We keep the database row because future orders
     * may reference this listing.
     */
    public void deleteListing(
            String merchantId,
            String listingId,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        MerchantListing listing =
                getListingOrThrow(
                        merchantId,
                        listingId
                );


        listing.setActive(false);


        listingRepo.save(listing);


        log.info(
                "Disabled listing {} for merchant {}",
                listingId,
                merchantId
        );
    }


    // =========================================================
    // API KEY MANAGEMENT
    // =========================================================

    /**
     * Generates a secure API key.
     *
     * The full key is returned once and only its hash is stored.
     */
    public ApiKeyResponse generateApiKey(
            String merchantId,
            CreateApiKeyRequest req,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        if (
                merchant.getStatus()
                        != Merchant.MerchantStatus.ACTIVE
        ) {

            throw new MerchantException(
                    "Merchant account must be ACTIVE to generate API keys"
            );
        }


        String envPrefix =
                req.getEnvironment()
                        == MerchantApiKey.KeyEnvironment.LIVE
                        ? "sp_live_"
                        : "sp_test_";


        byte[] randomBytes =
                new byte[32];


        SECURE_RANDOM.nextBytes(
                randomBytes
        );


        String rawKey =
                envPrefix
                        + Base64
                        .getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                randomBytes
                        );


        String keyPrefix =
                rawKey.substring(
                        0,
                        16
                );


        MerchantApiKey apiKey =
                apiKeyRepo.save(
                        MerchantApiKey.builder()

                                .merchant(
                                        merchant
                                )

                                .keyPrefix(
                                        keyPrefix
                                )

                                .keyHash(
                                        passwordEncoder.encode(
                                                rawKey
                                        )
                                )

                                .label(
                                        req.getLabel()
                                )

                                .environment(
                                        req.getEnvironment()
                                )

                                .expiresAt(
                                        req.getExpiresAt()
                                )

                                .build()
                );


        log.info(
                "Generated API key {} for merchant {}",
                keyPrefix,
                merchantId
        );


        return ApiKeyResponse.builder()

                .id(
                        apiKey.getId()
                )

                .merchantId(
                        merchantId
                )

                .keyPrefix(
                        keyPrefix
                )

                .fullKey(
                        rawKey
                )

                .label(
                        apiKey.getLabel()
                )

                .environment(
                        apiKey.getEnvironment()
                )

                .active(
                        apiKey.getActive()
                )

                .expiresAt(
                        apiKey.getExpiresAt()
                )

                .createdAt(
                        apiKey.getCreatedAt()
                )

                .build();
    }


    public void revokeApiKey(
            String keyId,
            String merchantId,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        MerchantApiKey key =
                apiKeyRepo.findById(keyId)

                        .orElseThrow(
                                () ->
                                        new MerchantException(
                                                "API key not found: "
                                                        + keyId
                                        )
                        );


        key.setActive(false);


        apiKeyRepo.save(key);


        log.info(
                "Revoked API key {} for merchant {}",
                keyId,
                merchantId
        );
    }


    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(
            String merchantId,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        return apiKeyRepo
                .findByMerchantIdAndActiveTrue(
                        merchantId
                )
                .stream()

                .map(
                        key ->
                                ApiKeyResponse.builder()

                                        .id(
                                                key.getId()
                                        )

                                        .merchantId(
                                                merchantId
                                        )

                                        .keyPrefix(
                                                key.getKeyPrefix()
                                        )

                                        .label(
                                                key.getLabel()
                                        )

                                        .environment(
                                                key.getEnvironment()
                                        )

                                        .active(
                                                key.getActive()
                                        )

                                        .expiresAt(
                                                key.getExpiresAt()
                                        )

                                        .createdAt(
                                                key.getCreatedAt()
                                        )

                                        .build()
                )

                .collect(Collectors.toList());
    }


    // =========================================================
    // MERCHANT PAYMENTS
    // =========================================================

    public MerchantPaymentResponse chargeCustomer(
            String merchantId,
            MerchantPaymentRequest req,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        if (
                merchant.getStatus()
                        != Merchant.MerchantStatus.ACTIVE
        ) {

            throw new MerchantException(
                    "Merchant account is not active"
            );
        }


        // =====================================================
        // IDEMPOTENCY
        // =====================================================

        if (
                req.getMerchantReference() != null
                        && paymentRepo
                        .existsByMerchantReferenceAndMerchantId(
                                req.getMerchantReference(),
                                merchantId
                        )
        ) {

            throw new MerchantException(
                    "Duplicate payment — reference already processed: "
                            + req.getMerchantReference()
            );
        }


        // =====================================================
        // FEES
        // =====================================================

        BigDecimal feeAmount =
                req.getAmount()
                        .multiply(FEE_RATE)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        BigDecimal netAmount =
                req.getAmount()
                        .subtract(
                                feeAmount
                        );


        MerchantPayment payment =
                paymentRepo.save(
                        MerchantPayment.builder()

                                .merchant(
                                        merchant
                                )

                                .payerUserId(
                                        req.getPayerUserId()
                                )

                                .amount(
                                        req.getAmount()
                                )

                                .description(
                                        req.getDescription()
                                )

                                .merchantReference(
                                        req.getMerchantReference()
                                )

                                .feeAmount(
                                        feeAmount
                                )

                                .netAmount(
                                        netAmount
                                )

                                .build()
                );


        // =====================================================
        // MOVE MONEY
        // =====================================================

        try {

            debitWallet(
                    req.getPayerUserId(),
                    req.getAmount(),
                    payment.getId(),
                    "Payment to "
                            + merchant.getBusinessName()
            );


            creditWallet(
                    merchant.getId(),
                    netAmount,
                    payment.getId(),
                    "Payment from customer (net of fee)"
            );


            payment.setStatus(
                    MerchantPayment.PaymentStatus.COMPLETED
            );


            payment.setPaymentTransactionId(
                    payment.getId()
            );

        } catch (Exception e) {

            payment.setStatus(
                    MerchantPayment.PaymentStatus.FAILED
            );


            paymentRepo.save(payment);


            throw new MerchantException(
                    "Payment failed: "
                            + e.getMessage()
            );
        }


        paymentRepo.save(payment);


        log.info(
                "Merchant payment {} completed: R{} from {} to {}",
                payment.getId(),
                req.getAmount(),
                req.getPayerUserId(),
                merchantId
        );


        return toPaymentResponse(payment);
    }


    // =========================================================
    // REFUNDS
    // =========================================================

    public MerchantPaymentResponse refundPayment(
            String paymentId,
            String merchantId,
            String ownerUserId
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);

        assertOwner(
                merchant,
                ownerUserId
        );


        MerchantPayment payment =
                paymentRepo.findById(
                        paymentId
                )
                        .orElseThrow(
                                () ->
                                        new MerchantException(
                                                "Payment not found: "
                                                        + paymentId
                                        )
                        );


        /*
         * Important authorization/integrity check:
         * owning Merchant A must never allow a payment that belongs
         * to Merchant B to be refunded.
         */
        if (
                payment.getMerchant() == null
                        || !payment.getMerchant()
                        .getId()
                        .equals(merchantId)
        ) {

            throw new MerchantException(
                    "Payment does not belong to this merchant"
            );
        }


        if (
                payment.getStatus()
                        != MerchantPayment.PaymentStatus.COMPLETED
        ) {

            throw new MerchantException(
                    "Only completed payments can be refunded"
            );
        }


        /*
         * A payment may be a legacy merchant payment or a marketplace
         * payment. Marketplace payments are linked to MerchantOrder
         * through MerchantOrder.paymentId.
         */
        MerchantOrder marketplaceOrder =
                merchantOrderRepo
                        .findByPaymentIdAndMerchantId(
                                paymentId,
                                merchantId
                        )
                        .orElse(null);


        /*
         * Validate marketplace order state BEFORE moving money.
         *
         * Refunds are valid after successful payment, whether the
         * merchant has not started fulfilment yet, is processing it,
         * or has already completed it.
         */
        if (marketplaceOrder != null) {

            MerchantOrder.OrderStatus orderStatus =
                    marketplaceOrder.getStatus();

            boolean refundableOrderStatus =
                    orderStatus
                            == MerchantOrder.OrderStatus.PAID
                            || orderStatus
                            == MerchantOrder.OrderStatus.PROCESSING
                            || orderStatus
                            == MerchantOrder.OrderStatus.COMPLETED;

            if (!refundableOrderStatus) {

                throw new MerchantException(
                        "Marketplace order cannot be refunded from status "
                                + orderStatus
                );
            }
        }


        try {

            /*
             * Reverse the original marketplace/merchant settlement:
             *
             * merchant loses the net amount it originally received;
             * customer receives the full amount they originally paid.
             */
            debitWallet(
                    merchant.getId(),
                    payment.getNetAmount(),
                    payment.getId(),
                    "Refund to customer"
            );


            creditWallet(
                    payment.getPayerUserId(),
                    payment.getAmount(),
                    payment.getId(),
                    "Refund from "
                            + merchant.getBusinessName()
            );


            payment.setStatus(
                    MerchantPayment.PaymentStatus.REFUNDED
            );

        } catch (Exception e) {

            throw new MerchantException(
                    "Refund failed: "
                            + e.getMessage()
            );
        }


        paymentRepo.save(payment);


        /*
         * Marketplace integration:
         * the customer order history reads MerchantOrder.status,
         * so update it in the same merchant-service transaction.
         *
         * Stock is intentionally NOT restored here. A financial refund
         * and a physical product return/restock are separate actions.
         */
        if (marketplaceOrder != null) {

            marketplaceOrder.setStatus(
                    MerchantOrder.OrderStatus.REFUNDED
            );

            merchantOrderRepo.save(
                    marketplaceOrder
            );


            log.info(
                    "Refunded marketplace payment {} and marked merchant order {} as REFUNDED",
                    paymentId,
                    marketplaceOrder.getId()
            );

        } else {

            log.info(
                    "Refunded non-marketplace merchant payment {} for merchant {}",
                    paymentId,
                    merchantId
            );
        }


        return toPaymentResponse(payment);
    }


    // =========================================================
    // PAYMENT HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<MerchantPaymentResponse> getPayments(
            String merchantId,
            String ownerUserId,
            int page,
            int size
    ) {

        Merchant merchant =
                getMerchantOrThrow(merchantId);


        assertOwner(
                merchant,
                ownerUserId
        );


        return paymentRepo
                .findByMerchantIdOrderByCreatedAtDesc(
                        merchantId,
                        PageRequest.of(
                                page,
                                size
                        )
                )
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private Merchant getMerchantOrThrow(
            String id
    ) {

        return merchantRepo
                .findById(id)

                .orElseThrow(
                        () ->
                                new MerchantException(
                                        "Merchant not found: "
                                                + id
                                )
                );
    }


    private MerchantListing getListingOrThrow(
            String merchantId,
            String listingId
    ) {

        return listingRepo
                .findByIdAndMerchantId(
                        listingId,
                        merchantId
                )

                .orElseThrow(
                        () ->
                                new MerchantException(
                                        "Listing not found: "
                                                + listingId
                                )
                );
    }


    private void assertOwner(
            Merchant merchant,
            String userId
    ) {

        if (
                !merchant
                        .getOwnerUserId()
                        .equals(userId)
        ) {

            throw new MerchantException(
                    "You do not own this merchant account"
            );
        }
    }


    private void debitWallet(
            String userId,
            BigDecimal amount,
            String refId,
            String desc
    ) {

        var body =
                new java.util.HashMap<String, Object>();


        body.put(
                "amount",
                amount
        );

        body.put(
                "referenceId",
                refId
        );

        body.put(
                "description",
                desc
        );


        restTemplate.postForEntity(
                walletServiceUrl
                        + "/internal/wallets/"
                        + userId
                        + "/debit",
                body,
                Object.class
        );
    }


    private void creditWallet(
            String userId,
            BigDecimal amount,
            String refId,
            String desc
    ) {

        var body =
                new java.util.HashMap<String, Object>();


        body.put(
                "amount",
                amount
        );

        body.put(
                "referenceId",
                refId
        );

        body.put(
                "description",
                desc
        );


        restTemplate.postForEntity(
                walletServiceUrl
                        + "/internal/wallets/"
                        + userId
                        + "/credit",
                body,
                Object.class
        );
    }


    // =========================================================
    // MAPPERS
    // =========================================================

    private MerchantResponse toMerchantResponse(
            Merchant merchant
    ) {

        return MerchantResponse.builder()

                .id(
                        merchant.getId()
                )

                .ownerUserId(
                        merchant.getOwnerUserId()
                )

                .businessName(
                        merchant.getBusinessName()
                )

                .businessRegistrationNumber(
                        merchant.getBusinessRegistrationNumber()
                )

                .category(
                        merchant.getCategory()
                )

                .businessEmail(
                        merchant.getBusinessEmail()
                )

                .businessPhone(
                        merchant.getBusinessPhone()
                )

                .description(
                        merchant.getDescription()
                )

                .logoUrl(
                        merchant.getLogoUrl()
                )

                .status(
                        merchant.getStatus()
                )

                .walletId(
                        merchant.getWalletId()
                )

                .createdAt(
                        merchant.getCreatedAt()
                )

                .build();
    }


    private MerchantPaymentResponse toPaymentResponse(
            MerchantPayment payment
    ) {

        return MerchantPaymentResponse.builder()

                .id(
                        payment.getId()
                )

                .merchantId(
                        payment.getMerchant()
                                .getId()
                )

                .payerUserId(
                        payment.getPayerUserId()
                )

                .amount(
                        payment.getAmount()
                )

                .currency(
                        payment.getCurrency()
                )

                .description(
                        payment.getDescription()
                )

                .merchantReference(
                        payment.getMerchantReference()
                )

                .paymentTransactionId(
                        payment.getPaymentTransactionId()
                )

                .status(
                        payment.getStatus()
                )

                .feeAmount(
                        payment.getFeeAmount()
                )

                .netAmount(
                        payment.getNetAmount()
                )

                .createdAt(
                        payment.getCreatedAt()
                )

                .build();
    }


    private ListingResponse toListingResponse(
            MerchantListing listing
    ) {

        return ListingResponse.builder()

                .id(
                        listing.getId()
                )

                .merchantId(
                        listing.getMerchant()
                                .getId()
                )

                .merchantName(
                        listing.getMerchant()
                                .getBusinessName()
                )

                .title(
                        listing.getTitle()
                )

                .description(
                        listing.getDescription()
                )

                .price(
                        listing.getPrice()
                )

                .type(
                        listing.getType()
                )

                .imageUrl(
                        listing.getImageUrl()
                )

                .stockQuantity(
                        listing.getStockQuantity()
                )

                .active(
                        listing.getActive()
                )

                .createdAt(
                        listing.getCreatedAt()
                )

                .updatedAt(
                        listing.getUpdatedAt()
                )

                .build();
    }
}