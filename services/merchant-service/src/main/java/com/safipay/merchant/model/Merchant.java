package com.safipay.merchant.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String email;
    private String category;

    private UUID walletId;

    private String status; // PENDING, APPROVED
}
