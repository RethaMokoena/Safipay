package com.safipay.paymentgateway.client;

import com.safipay.paymentgateway.dto.FraudCheckRequest;
import com.safipay.paymentgateway.dto.FraudCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "fraud-service", url = "${clients.fraud.url:http://localhost:8083}")
public interface FraudClient {
    @PostMapping("/fraud/check")
    FraudCheckResponse check(FraudCheckRequest req);
}
