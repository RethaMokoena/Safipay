package com.safipay.merchant.service;

import com.safipay.merchant.client.WalletClient;
import com.safipay.merchant.dto.MerchantRequest;
import com.safipay.merchant.dto.MerchantResponse;
import com.safipay.merchant.model.Merchant;
import com.safipay.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository repo;
    private final WalletClient walletClient;

    public MerchantResponse register(MerchantRequest req) {

        var wallet = walletClient.createWallet();

        Merchant merchant = Merchant.builder()
                .name(req.getName())
                .email(req.getEmail())
                .category(req.getCategory())
                .walletId(wallet.getWalletId())
                .status("APPROVED")
                .build();

        merchant = repo.save(merchant);

        return MerchantResponse.builder()
                .merchantId(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .category(merchant.getCategory())
                .walletId(merchant.getWalletId())
                .status(merchant.getStatus())
                .build();
    }

    public MerchantResponse findById(UUID id) {
        Merchant m = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        return MerchantResponse.builder()
                .merchantId(m.getId())
                .name(m.getName())
                .email(m.getEmail())
                .status(m.getStatus())
                .walletId(m.getWalletId())
                .category(m.getCategory())
                .build();
    }

}
