package com.safipay.paymentgateway.controller;

import com.safipay.paymentgateway.dto.*;
import com.safipay.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> process(
            @RequestBody PaymentRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        PaymentResponse resp = paymentService.processPayment(req, idempotencyKey);
        return ResponseEntity.ok(resp);
    }
}
