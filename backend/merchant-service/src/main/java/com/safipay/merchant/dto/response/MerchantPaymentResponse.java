package com.safipay.merchant.dto.response;
import com.safipay.merchant.model.MerchantPayment;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;

@Data @Builder
public class MerchantPaymentResponse {
    private String id; private String merchantId; private String payerUserId;
    private BigDecimal amount; private String currency; private String description;
    private String merchantReference; private String paymentTransactionId;
    private MerchantPayment.PaymentStatus status;
    private BigDecimal feeAmount; private BigDecimal netAmount;
    private LocalDateTime createdAt;
}
