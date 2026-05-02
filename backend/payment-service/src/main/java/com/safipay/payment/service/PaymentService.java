package com.safipay.payment.service;

import com.safipay.payment.dto.request.RequestMoneyRequest;
import com.safipay.payment.dto.request.SendMoneyRequest;
import com.safipay.payment.dto.response.PaymentResponse;
import com.safipay.payment.exception.PaymentException;
import com.safipay.payment.model.Payment;
import com.safipay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final WalletClient walletClient;
    private final FraudClient fraudClient;
    private final LedgerClient ledgerClient;
    private final WebhookClient webhookClient;

    // ── Send Money ────────────────────────────────────────────────

    public PaymentResponse sendMoney(String senderUserId, SendMoneyRequest req) {
        if (senderUserId.equals(req.getRecipientUserId())) {
            throw new PaymentException("Cannot send money to yourself");
        }

        Payment payment = paymentRepo.save(Payment.builder()
                .senderUserId(senderUserId)
                .recipientUserId(req.getRecipientUserId())
                .amount(req.getAmount())
                .description(req.getDescription())
                .referenceNote(req.getReferenceNote())
                .status(Payment.PaymentStatus.PENDING)
                .type(Payment.PaymentType.SEND_MONEY)
                .build());

        // Fraud check
        String fraudDecision = fraudClient.evaluate(
                payment.getId(), senderUserId, req.getAmount(),
                "PEER_TRANSFER", req.getRecipientUserId());

        if ("BLOCKED".equals(fraudDecision)) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            throw new PaymentException("Transaction blocked by fraud prevention system");
        }

        // Wallet debit/credit
        try {
            walletClient.debit(senderUserId, req.getAmount(), payment.getId(),
                    req.getDescription() != null ? req.getDescription() : "Transfer");
            walletClient.credit(req.getRecipientUserId(), req.getAmount(), payment.getId(),
                    req.getDescription() != null ? req.getDescription() : "Transfer received");
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
        } catch (Exception e) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            throw new PaymentException("Payment failed: " + e.getMessage());
        }

        paymentRepo.save(payment);
        ledgerClient.postTransfer(senderUserId, req.getRecipientUserId(),
                req.getAmount(), payment.getId(),
                req.getDescription() != null ? req.getDescription() : "P2P Transfer");

        webhookClient.firePaymentEvent("payment.completed", senderUserId,
                payment.getId(), req.getAmount(), "COMPLETED");

        log.info("Payment {} completed: R{} {} → {}",
                payment.getId(), req.getAmount(), senderUserId, req.getRecipientUserId());
        return toResponse(payment);
    }

    // ── Request Money ─────────────────────────────────────────────

    public PaymentResponse requestMoney(String requestorUserId, RequestMoneyRequest req) {
        Payment payment = paymentRepo.save(Payment.builder()
                .senderUserId(req.getFromUserId())
                .recipientUserId(requestorUserId)
                .amount(req.getAmount())
                .description(req.getDescription())
                .referenceNote(req.getReferenceNote())
                .status(Payment.PaymentStatus.PENDING)
                .type(Payment.PaymentType.REQUEST_MONEY)
                .build());

        log.info("Money request {} created: R{} from {}", payment.getId(), req.getAmount(), req.getFromUserId());
        return toResponse(payment);
    }

    public PaymentResponse approveRequest(String paymentId, String payerUserId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment request not found: " + paymentId));

        if (payment.getType() != Payment.PaymentType.REQUEST_MONEY)
            throw new PaymentException("Not a money request");
        if (!payment.getSenderUserId().equals(payerUserId))
            throw new PaymentException("You are not the payer for this request");
        if (payment.getStatus() != Payment.PaymentStatus.PENDING)
            throw new PaymentException("Request is no longer pending: " + payment.getStatus());

        String fraudDecision = fraudClient.evaluate(payment.getId(), payerUserId,
                payment.getAmount(), "PEER_TRANSFER", payment.getRecipientUserId());
        if ("BLOCKED".equals(fraudDecision)) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            throw new PaymentException("Transaction blocked by fraud prevention");
        }

        try {
            walletClient.debit(payerUserId, payment.getAmount(), payment.getId(), "Approved payment request");
            walletClient.credit(payment.getRecipientUserId(), payment.getAmount(), payment.getId(), "Payment request approved");
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
        } catch (Exception e) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            throw new PaymentException("Could not process approval: " + e.getMessage());
        }

        paymentRepo.save(payment);
        ledgerClient.postTransfer(payerUserId, payment.getRecipientUserId(),
                payment.getAmount(), payment.getId(), "Approved money request");
        return toResponse(payment);
    }

    public PaymentResponse declineRequest(String paymentId, String payerUserId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment request not found: " + paymentId));
        if (!payment.getSenderUserId().equals(payerUserId))
            throw new PaymentException("You are not the payer for this request");
        if (payment.getStatus() != Payment.PaymentStatus.PENDING)
            throw new PaymentException("Request is no longer pending");

        payment.setStatus(Payment.PaymentStatus.FAILED);
        paymentRepo.save(payment);
        return toResponse(payment);
    }

    // ── Refund ────────────────────────────────────────────────────

    public PaymentResponse refund(String paymentId, String requestingUserId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED)
            throw new PaymentException("Only completed payments can be refunded");
        if (!payment.getSenderUserId().equals(requestingUserId)
                && !payment.getRecipientUserId().equals(requestingUserId))
            throw new PaymentException("You are not a participant in this payment");

        try {
            walletClient.debit(payment.getRecipientUserId(), payment.getAmount(),
                    payment.getId(), "Refund of payment " + paymentId);
            walletClient.credit(payment.getSenderUserId(), payment.getAmount(),
                    payment.getId(), "Refund received");
            payment.setStatus(Payment.PaymentStatus.REVERSED);
        } catch (Exception e) {
            throw new PaymentException("Refund failed: " + e.getMessage());
        }

        paymentRepo.save(payment);
        ledgerClient.postReversal(payment.getRecipientUserId(), payment.getSenderUserId(),
                payment.getAmount(), paymentId);

        webhookClient.firePaymentEvent("payment.refunded", payment.getSenderUserId(), paymentId, payment.getAmount(), "REVERSED");
        log.info("Payment {} refunded", paymentId);
        return toResponse(payment);
    }

    // ── Queries ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PaymentResponse> getHistory(String userId, int page, int size) {
        return paymentRepo.findByUserId(userId, PageRequest.of(page, size))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPendingRequests(String userId) {
        return paymentRepo.findPendingRequestsForUser(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(String paymentId) {
        return toResponse(paymentRepo.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId)));
    }

    // ── Internal ──────────────────────────────────────────────────

    public PaymentResponse processInternalPayment(String senderUserId, String recipientUserId,
            BigDecimal amount, String description, String externalRef, Payment.PaymentType type) {
        Payment payment = paymentRepo.save(Payment.builder()
                .senderUserId(senderUserId).recipientUserId(recipientUserId)
                .amount(amount).description(description)
                .status(Payment.PaymentStatus.PENDING).type(type).build());
        try {
            walletClient.debit(senderUserId, amount, payment.getId(), description);
            walletClient.credit(recipientUserId, amount, payment.getId(), description);
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
        } catch (Exception e) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            throw new PaymentException("Internal payment failed: " + e.getMessage());
        }
        paymentRepo.save(payment);
        ledgerClient.postTransfer(senderUserId, recipientUserId, amount, payment.getId(), description);
        return toResponse(payment);
    }

    // ── Mapper ────────────────────────────────────────────────────

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId()).senderUserId(p.getSenderUserId())
                .recipientUserId(p.getRecipientUserId()).amount(p.getAmount())
                .currency(p.getCurrency()).description(p.getDescription())
                .status(p.getStatus()).type(p.getType()).createdAt(p.getCreatedAt()).build();
    }
}
