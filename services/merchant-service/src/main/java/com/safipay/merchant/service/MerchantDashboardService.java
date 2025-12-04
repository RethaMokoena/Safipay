package com.safipay.merchant.service;

import com.safipay.merchant.client.WalletClient;
import com.safipay.merchant.client.PaymentClient;
import com.safipay.merchant.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantDashboardService {

    private final WalletClient walletClient;
    private final PaymentClient paymentClient;

    public MerchantDashboardResponse getDashboard(UUID merchantId, UUID walletId) {

        // 1. Get wallet balance
        BigDecimal balance = walletClient.getBalance(walletId).getBalance();

        // 2. Fetch all payments for this merchant
        List<PaymentSummary> payments = paymentClient.getPayments(merchantId);

        // 3. Calculate total revenue (SUM success payments)
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .map(PaymentSummary::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Recent payments (last 5)
        List<PaymentSummary> recentPayments =
                payments.stream()
                        .sorted(Comparator.comparing(PaymentSummary::getCreatedAt).reversed())
                        .limit(5)
                        .collect(Collectors.toList());

        // 5. Daily revenue analytics (last 7 days)
        LocalDate today = LocalDate.now();

        List<DailyRevenue> analytics =
                payments.stream()
                        .filter(p -> "SUCCESS".equals(p.getStatus()))
                        .collect(Collectors.groupingBy(
                                p -> p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                                Collectors.reducing(BigDecimal.ZERO, PaymentSummary::getAmount, BigDecimal::add)
                        ))
                        .entrySet()
                        .stream()
                        .filter(e -> !e.getKey().isBefore(today.minusDays(7)))
                        .map(e -> DailyRevenue.builder()
                                .date(e.getKey())
                                .totalAmount(e.getValue())
                                .build())
                        .sorted(Comparator.comparing(DailyRevenue::getDate))
                        .collect(Collectors.toList());

        return MerchantDashboardResponse.builder()
                .currentBalance(balance)
                .totalRevenue(totalRevenue)
                .recentPayments(recentPayments)
                .analytics(analytics)
                .build();
    }
}
