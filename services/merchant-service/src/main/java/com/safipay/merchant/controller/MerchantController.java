package com.safipay.merchant.controller;

import com.safipay.merchant.dto.MerchantRequest;
import com.safipay.merchant.dto.MerchantResponse;
import com.safipay.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/register")
    public MerchantResponse register(@RequestBody MerchantRequest req) {
        return merchantService.register(req);
    }

    @GetMapping("/info/{id}")
    public MerchantResponse getInfo(@PathVariable UUID id) {
        var m = merchantService.findById(id);
        return m;
    }

}
