package com.safipay.paymentgateway.client;

import com.safipay.paymentgateway.dto.DebitRequest;
import com.safipay.paymentgateway.dto.WalletResponse;
import com.safipay.paymentgateway.dto.CreditRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "wallet-service", url = "${clients.wallet.url:http://localhost:8081}")
public interface WalletClient {

    @PostMapping("/wallet/debit")
    WalletResponse debit(DebitRequest req);

    @PostMapping("/wallet/credit")
    WalletResponse credit(CreditRequest req);
}
