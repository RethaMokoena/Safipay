package com.safipay.paymentgateway.client;

import com.safipay.paymentgateway.dto.MerchantInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "merchant-service",
        url = "${clients.merchant.url:http://localhost:8084}"
)
public interface MerchantClient {

    @GetMapping("/merchant/info/{merchantId}")
    MerchantInfoResponse getMerchant(@PathVariable("merchantId") String merchantId);
}
