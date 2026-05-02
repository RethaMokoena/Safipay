package com.safipay.payment.controller;

import com.safipay.payment.dto.response.*;
import com.safipay.payment.model.Payment;
import com.safipay.payment.service.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @Data
    public static class InternalPaymentRequest {
        private String senderUserId;
        private String recipientUserId;
        private BigDecimal amount;
        private String description;
        private String externalRef;
        private String type; // STOKVEL_CONTRIBUTION, STOKVEL_PAYOUT, MERCHANT_PAYMENT
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> process(
            @RequestBody InternalPaymentRequest req) {
        Payment.PaymentType type;
        try {
            type = Payment.PaymentType.valueOf(req.getType());
        } catch (IllegalArgumentException e) {
            type = Payment.PaymentType.SEND_MONEY;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                paymentService.processInternalPayment(req.getSenderUserId(),
                        req.getRecipientUserId(), req.getAmount(),
                        req.getDescription(), req.getExternalRef(), type)));
    }
}
