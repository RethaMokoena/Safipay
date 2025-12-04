package com.safipay.wallet.dto;

import lombok.*;

import java.util.UUID;

@Data
public class CreateWalletRequest {
    private UUID userId;
}

