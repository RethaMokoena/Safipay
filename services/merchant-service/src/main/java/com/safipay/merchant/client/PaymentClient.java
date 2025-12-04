package com.safipay.merchant.client;

import com.safipay.merchant.dto.PaymentSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "payment-gateway-service",
        url = "${clients.payment.url:http://localhost:8082}"
)
public interface PaymentClient {

    @GetMapping("/payment/merchant/{merchantId}")
    List<PaymentSummary> getPayments(@PathVariable("merchantId") UUID merchantId);
}
