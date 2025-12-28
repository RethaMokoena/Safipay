package com.safipay.merchant.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DailyRevenue {
    private LocalDate date;
    private BigDecimal totalAmount;
}
