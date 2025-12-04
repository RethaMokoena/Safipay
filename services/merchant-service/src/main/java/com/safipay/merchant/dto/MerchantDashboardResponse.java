package com.safipay.merchant.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MerchantDashboardResponse {

    private BigDecimal currentBalance;

    private BigDecimal totalRevenue;

    private List<PaymentSummary> recentPayments;

    private List<DailyRevenue> analytics;
}
