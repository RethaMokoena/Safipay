package com.safipay.merchant.repository;

import com.safipay.merchant.model.Merchant;
import com.safipay.merchant.model.MerchantListing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@Repository
public interface MerchantListingRepository
        extends JpaRepository<MerchantListing, String> {


    // =========================================================
    // MERCHANT LISTING MANAGEMENT
    // =========================================================

    /**
     * All listings belonging to a merchant.
     *
     * Used by the merchant dashboard.
     * Includes active and inactive listings.
     */
    List<MerchantListing> findByMerchantId(
            String merchantId
    );


    /**
     * Only active listings belonging to a merchant.
     *
     * Used by customers viewing a merchant storefront.
     */
    List<MerchantListing> findByMerchantIdAndActiveTrue(
            String merchantId
    );


    /**
     * Find one listing while also confirming
     * that it belongs to the supplied merchant.
     */
    Optional<MerchantListing> findByIdAndMerchantId(
            String listingId,
            String merchantId
    );


    // =========================================================
    // GLOBAL MARKETPLACE
    // =========================================================

    /**
     * Global marketplace discovery.
     *
     * Supports:
     *
     * search text
     * merchant category
     * PRODUCT / SERVICE
     * maximum price
     *
     * Only active listings belonging to ACTIVE merchants
     * can appear in marketplace results.
     */
    @Query("""
        SELECT l
        FROM MerchantListing l
        JOIN l.merchant m
        WHERE l.active = true
          AND m.status = :merchantStatus
          AND (:category IS NULL OR m.category = :category)
          AND (:type IS NULL OR l.type = :type)
          AND (:maxPrice IS NULL OR l.price <= :maxPrice)
          AND (
                :query = ''
                OR LOWER(l.title)
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(l.description, ''))
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(m.businessName)
                    LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY l.createdAt DESC
        """)
    List<MerchantListing> discoverListings(

            @Param("query")
            String query,

            @Param("category")
            Merchant.BusinessCategory category,

            @Param("type")
            MerchantListing.ListingType type,

            @Param("maxPrice")
            BigDecimal maxPrice,

            @Param("merchantStatus")
            Merchant.MerchantStatus merchantStatus
    );


    // =========================================================
    // BASIC MARKETPLACE QUERIES
    // =========================================================

    /**
     * All active listings.
     */
    List<MerchantListing> findByActiveTrue();


    /**
     * Active listings filtered by listing type.
     */
    List<MerchantListing> findByTypeAndActiveTrue(
            MerchantListing.ListingType type
    );


    /**
     * Active listings costing less than
     * or equal to the supplied price.
     */
    List<MerchantListing> findByPriceLessThanEqualAndActiveTrue(
            BigDecimal maxPrice
    );


    // =========================================================
    // TEXT SEARCH
    // =========================================================

    /**
     * Search active listings by title or description.
     */
    @Query("""
        SELECT l
        FROM MerchantListing l
        WHERE l.active = true
          AND (
                LOWER(l.title)
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(l.description, ''))
                    LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    List<MerchantListing> searchActiveListings(
            @Param("query")
            String query
    );


    /**
     * Search active listings while also applying
     * a maximum price.
     */
    @Query("""
        SELECT l
        FROM MerchantListing l
        WHERE l.active = true
          AND l.price <= :maxPrice
          AND (
                LOWER(l.title)
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(l.description, ''))
                    LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    List<MerchantListing> searchActiveListingsUnderPrice(

            @Param("query")
            String query,

            @Param("maxPrice")
            BigDecimal maxPrice
    );
}