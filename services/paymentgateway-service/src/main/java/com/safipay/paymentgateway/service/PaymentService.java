package com.safipay.paymentgateway.service;

import com.safipay.paymentgateway.client.FraudClient;
import com.safipay.paymentgateway.client.MerchantClient;
import com.safipay.paymentgateway.client.WalletClient;
import com.safipay.paymentgateway.dto.*;
import com.safipay.paymentgateway.model.Payment;
import com.safipay.paymentgateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final FraudClient fraudClient;
    private final WalletClient walletClient;
    private final MerchantClient merchantClient;
    private final PaymentRepository paymentRepo;


    /**
     * Process payment with idempotencyKey (may be null).
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest req, String idempotencyKey) {

        // IDEMPOTENCY CHECK
        if (idempotencyKey != null) {
            Optional<Payment> existing = paymentRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Payment p = existing.get();
                return PaymentResponse.builder()
                        .paymentId(p.getId())
                        .status(p.getStatus())
                        .message(p.getMessage())
                        .build();
            }
        }

        // 1️⃣ MERCHANT LOOKUP
        MerchantInfoResponse merchant;
        try {
            merchant = merchantClient.getMerchant(req.getMerchantId().toString());
        } catch (Exception ex) {
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("merchant-not-found")
                    .build();
        }

        if (!"APPROVED".equals(merchant.getStatus())) {
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("merchant-not-approved")
                    .build();
        }

        UUID merchantWalletId = merchant.getWalletId();

        // Create Payment
        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .payerWalletId(req.getPayerWalletId())
                .merchantWalletId(merchantWalletId)
                .amount(req.getAmount())
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        payment = paymentRepo.save(payment);

        // 2️⃣ FRAUD CHECK
        FraudCheckRequest fraudReq = new FraudCheckRequest();
        fraudReq.setWalletId(req.getPayerWalletId());
        fraudReq.setAmount(req.getAmount());
        fraudReq.setPaymentType("PAYMENT");

        FraudCheckResponse fraudResp;
        try {
            fraudResp = fraudClient.check(fraudReq);
        } catch (Exception e) {
            payment.setStatus("FAILED");
            payment.setMessage("fraud-service-unavailable");
            paymentRepo.save(payment);
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status("FAILED")
                    .message(payment.getMessage())
                    .build();
        }

        if (fraudResp.isFraudulent()) {
            payment.setStatus("FAILED");
            payment.setMessage("fraud-detected");
            paymentRepo.save(payment);
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status("FAILED")
                    .message("fraud-detected")
                    .build();
        }

        // 3️⃣ DEBIT PAYER
        try {
            walletClient.debit(new DebitRequest(req.getPayerWalletId(), req.getAmount()));
        } catch (Exception e) {
            payment.setStatus("FAILED");
            payment.setMessage("debit-failed");
            paymentRepo.save(payment);
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status("FAILED")
                    .message("debit-failed")
                    .build();
        }

        // 4️⃣ CREDIT MERCHANT
        try {
            walletClient.credit(new CreditRequest(merchantWalletId, req.getAmount()));
        } catch (Exception ex) {

            // COMPENSATION: refund payer
            walletClient.credit(new CreditRequest(req.getPayerWalletId(), req.getAmount()));

            payment.setStatus("FAILED");
            payment.setMessage("merchant-credit-failed-rollback-successful");
            paymentRepo.save(payment);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status("FAILED")
                    .message("merchant-credit-failed")
                    .build();
        }

        // SUCCESS
        payment.setStatus("SUCCESS");
        payment.setMessage("payment-completed");
        paymentRepo.save(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status("SUCCESS")
                .message("payment-completed")
                .build();
    }

}
