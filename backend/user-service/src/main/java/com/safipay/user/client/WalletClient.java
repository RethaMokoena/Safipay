package com.safipay.user.client;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class WalletClient {

    private final RestClient restClient;

    public WalletClient(
            RestClient.Builder builder,
            @Value("${WALLET_SERVICE_URL:http://localhost:8082}")
            String walletServiceUrl
    ) {
        this.restClient = builder
            .baseUrl(walletServiceUrl)
            .build();
    }

   public void createWallet(
        String userId,
        String email
) {

    log.info(
            "Requesting wallet creation for userId={}",
            userId
    );

    restClient.post()
            .uri(
                    "/internal/wallets/{userId}/create",
                    userId
            )
            .body(
                    Map.of(
                            "email",
                            email
                    )
            )
            .retrieve()
            .toBodilessEntity();

    log.info(
            "Wallet creation completed for userId={}",
            userId
    );
}
}