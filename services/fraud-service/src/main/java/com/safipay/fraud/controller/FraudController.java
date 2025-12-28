package com.safipay.fraud.controller;

import com.safipay.fraud.dto.FraudCheckRequest;
import com.safipay.fraud.dto.FraudCheckResponse;
import com.safipay.fraud.service.FraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService service;

    @PostMapping("/check")
    public FraudCheckResponse check(@RequestBody FraudCheckRequest req) {
        return service.checkFraud(req);
    }
}
