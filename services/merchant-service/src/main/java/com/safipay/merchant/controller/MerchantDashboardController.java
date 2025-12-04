package com.safipay.merchant.controller;

import com.safipay.merchant.dto.MerchantDashboardResponse;
import com.safipay.merchant.service.MerchantDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/merchant/dashboard")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final MerchantDashboardService dashboardService;

    @GetMapping("/{merchantId}")
    public MerchantDashboardResponse getDashboard(@PathVariable UUID merchantId,
                                                  @RequestParam UUID walletId) {
        return dashboardService.getDashboard(merchantId, walletId);
    }
}
