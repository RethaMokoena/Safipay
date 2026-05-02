package com.safipay.merchant.service;

import com.safipay.merchant.dto.request.*;
import com.safipay.merchant.dto.response.*;
import com.safipay.merchant.exception.MerchantException;
import com.safipay.merchant.model.*;
import com.safipay.merchant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MerchantService {

    private final MerchantRepository merchantRepo;
    private final MerchantApiKeyRepository apiKeyRepo;
    private final MerchantPaymentRepository paymentRepo;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.wallet-service-url}")
    private String walletServiceUrl;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.015"); // 1.5%
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── Merchant Registration ─────────────────────────────────────

    public MerchantResponse registerMerchant(RegisterMerchantRequest req, String ownerUserId) {
        if (merchantRepo.existsByOwnerUserIdAndBusinessName(ownerUserId, req.getBusinessName())) {
            throw new MerchantException("You already have a merchant account with this business name");
        }

        Merchant merchant = merchantRepo.save(Merchant.builder()
                .ownerUserId(ownerUserId)
                .businessName(req.getBusinessName())
                .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                .category(req.getCategory())
                .businessEmail(req.getBusinessEmail())
                .businessPhone(req.getBusinessPhone())
                .description(req.getDescription())
                .build());

        // Auto-create merchant wallet via wallet-service internal endpoint
        try {
            restTemplate.postForEntity(
                    walletServiceUrl + "/internal/wallets/" + merchant.getId() + "/create",
                    null, Object.class);
            merchant.setWalletId(merchant.getId());
            merchantRepo.save(merchant);
        } catch (Exception e) {
            log.warn("Could not auto-create merchant wallet: {}", e.getMessage());
        }

        log.info("Registered merchant '{}' (id={}) for user {}", merchant.getBusinessName(), merchant.getId(), ownerUserId);
        return toMerchantResponse(merchant);
    }

    public MerchantResponse approveMerchant(String merchantId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        log.info("Approved merchant {}", merchantId);
        return toMerchantResponse(merchantRepo.save(merchant));
    }

    public MerchantResponse suspendMerchant(String merchantId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        merchant.setStatus(Merchant.MerchantStatus.SUSPENDED);
        log.warn("Suspended merchant {}", merchantId);
        return toMerchantResponse(merchantRepo.save(merchant));
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> getMyMerchants(String ownerUserId) {
        return merchantRepo.findByOwnerUserId(ownerUserId)
                .stream().map(this::toMerchantResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(String merchantId) {
        return toMerchantResponse(getMerchantOrThrow(merchantId));
    }

    // ── API Key Management ────────────────────────────────────────

    /**
     * Generates a secure API key. The full key is returned ONCE and then
     * only the hash is stored — similar to how GitHub/Stripe handle tokens.
     */
    public ApiKeyResponse generateApiKey(String merchantId, CreateApiKeyRequest req, String ownerUserId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        assertOwner(merchant, ownerUserId);

        if (merchant.getStatus() != Merchant.MerchantStatus.ACTIVE) {
            throw new MerchantException("Merchant account must be ACTIVE to generate API keys");
        }

        // Generate: sp_live_<32 random bytes base64url>
        String envPrefix = req.getEnvironment() == MerchantApiKey.KeyEnvironment.LIVE ? "sp_live_" : "sp_test_";
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawKey = envPrefix + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String keyPrefix = rawKey.substring(0, 16); // Show first 16 chars as identifier

        MerchantApiKey apiKey = apiKeyRepo.save(MerchantApiKey.builder()
                .merchant(merchant)
                .keyPrefix(keyPrefix)
                .keyHash(passwordEncoder.encode(rawKey))
                .label(req.getLabel())
                .environment(req.getEnvironment())
                .expiresAt(req.getExpiresAt())
                .build());

        log.info("Generated API key {} for merchant {}", keyPrefix, merchantId);

        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .merchantId(merchantId)
                .keyPrefix(keyPrefix)
                .fullKey(rawKey) // Only time full key is returned
                .label(apiKey.getLabel())
                .environment(apiKey.getEnvironment())
                .active(apiKey.getActive())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    public void revokeApiKey(String keyId, String merchantId, String ownerUserId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        assertOwner(merchant, ownerUserId);

        MerchantApiKey key = apiKeyRepo.findById(keyId)
                .orElseThrow(() -> new MerchantException("API key not found: " + keyId));
        key.setActive(false);
        apiKeyRepo.save(key);
        log.info("Revoked API key {} for merchant {}", keyId, merchantId);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(String merchantId, String ownerUserId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        assertOwner(merchant, ownerUserId);

        return apiKeyRepo.findByMerchantIdAndActiveTrue(merchantId)
                .stream()
                .map(k -> ApiKeyResponse.builder()
                        .id(k.getId()).merchantId(merchantId)
                        .keyPrefix(k.getKeyPrefix()) // fullKey NOT returned here
                        .label(k.getLabel()).environment(k.getEnvironment())
                        .active(k.getActive()).expiresAt(k.getExpiresAt())
                        .createdAt(k.getCreatedAt()).build())
                .collect(Collectors.toList());
    }

    // ── Merchant Payments ─────────────────────────────────────────

    public MerchantPaymentResponse chargeCustomer(String merchantId, MerchantPaymentRequest req, String ownerUserId) {
        Merchant merchant = getMerchantOrThrow(merchantId);

        if (merchant.getStatus() != Merchant.MerchantStatus.ACTIVE) {
            throw new MerchantException("Merchant account is not active");
        }

        // Idempotency check on merchant reference
        if (req.getMerchantReference() != null &&
                paymentRepo.existsByMerchantReferenceAndMerchantId(req.getMerchantReference(), merchantId)) {
            throw new MerchantException("Duplicate payment — reference already processed: " + req.getMerchantReference());
        }

        // Calculate fee and net
        BigDecimal feeAmount = req.getAmount().multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = req.getAmount().subtract(feeAmount);

        MerchantPayment payment = paymentRepo.save(MerchantPayment.builder()
                .merchant(merchant)
                .payerUserId(req.getPayerUserId())
                .amount(req.getAmount())
                .description(req.getDescription())
                .merchantReference(req.getMerchantReference())
                .feeAmount(feeAmount)
                .netAmount(netAmount)
                .build());

        // Debit payer wallet via wallet-service
        try {
            debitWallet(req.getPayerUserId(), req.getAmount(), payment.getId(),
                    "Payment to " + merchant.getBusinessName());
            creditWallet(merchant.getId(), netAmount, payment.getId(),
                    "Payment from customer (net of fee)");

            payment.setStatus(MerchantPayment.PaymentStatus.COMPLETED);
            payment.setPaymentTransactionId(payment.getId());
        } catch (Exception e) {
            payment.setStatus(MerchantPayment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            throw new MerchantException("Payment failed: " + e.getMessage());
        }

        paymentRepo.save(payment);
        log.info("Merchant payment {} completed: R{} from {} to {}",
                payment.getId(), req.getAmount(), req.getPayerUserId(), merchantId);
        return toPaymentResponse(payment);
    }

    public MerchantPaymentResponse refundPayment(String paymentId, String merchantId, String ownerUserId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        assertOwner(merchant, ownerUserId);

        MerchantPayment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new MerchantException("Payment not found: " + paymentId));

        if (payment.getStatus() != MerchantPayment.PaymentStatus.COMPLETED) {
            throw new MerchantException("Only completed payments can be refunded");
        }

        // Reverse: debit merchant, credit payer
        try {
            debitWallet(merchant.getId(), payment.getNetAmount(), payment.getId(), "Refund to customer");
            creditWallet(payment.getPayerUserId(), payment.getAmount(), payment.getId(), "Refund from " + merchant.getBusinessName());
            payment.setStatus(MerchantPayment.PaymentStatus.REFUNDED);
        } catch (Exception e) {
            throw new MerchantException("Refund failed: " + e.getMessage());
        }

        paymentRepo.save(payment);
        log.info("Refunded payment {} for merchant {}", paymentId, merchantId);
        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<MerchantPaymentResponse> getPayments(String merchantId, String ownerUserId, int page, int size) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        assertOwner(merchant, ownerUserId);
        return paymentRepo.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size))
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Merchant getMerchantOrThrow(String id) {
        return merchantRepo.findById(id)
                .orElseThrow(() -> new MerchantException("Merchant not found: " + id));
    }

    private void assertOwner(Merchant merchant, String userId) {
        if (!merchant.getOwnerUserId().equals(userId)) {
            throw new MerchantException("You do not own this merchant account");
        }
    }

    private void debitWallet(String userId, BigDecimal amount, String refId, String desc) {
        var body = new java.util.HashMap<String, Object>();
        body.put("amount", amount); body.put("referenceId", refId); body.put("description", desc);
        restTemplate.postForEntity(walletServiceUrl + "/internal/wallets/" + userId + "/debit", body, Object.class);
    }

    private void creditWallet(String userId, BigDecimal amount, String refId, String desc) {
        var body = new java.util.HashMap<String, Object>();
        body.put("amount", amount); body.put("referenceId", refId); body.put("description", desc);
        restTemplate.postForEntity(walletServiceUrl + "/internal/wallets/" + userId + "/credit", body, Object.class);
    }

    // ── Mappers ───────────────────────────────────────────────────

    private MerchantResponse toMerchantResponse(Merchant m) {
        return MerchantResponse.builder()
                .id(m.getId()).ownerUserId(m.getOwnerUserId())
                .businessName(m.getBusinessName())
                .businessRegistrationNumber(m.getBusinessRegistrationNumber())
                .category(m.getCategory()).businessEmail(m.getBusinessEmail())
                .businessPhone(m.getBusinessPhone()).description(m.getDescription())
                .logoUrl(m.getLogoUrl()).status(m.getStatus()).walletId(m.getWalletId())
                .createdAt(m.getCreatedAt()).build();
    }

    private MerchantPaymentResponse toPaymentResponse(MerchantPayment p) {
        return MerchantPaymentResponse.builder()
                .id(p.getId()).merchantId(p.getMerchant().getId())
                .payerUserId(p.getPayerUserId()).amount(p.getAmount())
                .currency(p.getCurrency()).description(p.getDescription())
                .merchantReference(p.getMerchantReference())
                .paymentTransactionId(p.getPaymentTransactionId())
                .status(p.getStatus()).feeAmount(p.getFeeAmount()).netAmount(p.getNetAmount())
                .createdAt(p.getCreatedAt()).build();
    }
}
