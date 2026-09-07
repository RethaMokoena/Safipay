package com.safipay.merchant.dto.request;

import com.safipay.merchant.model.MerchantOrder;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMerchantOrderStatusRequest {

    @NotNull
    private MerchantOrder.OrderStatus status;
}