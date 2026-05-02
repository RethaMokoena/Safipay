package com.safipay.payment.dto.response;
import com.safipay.payment.model.Payment;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentResponse {
    private String id;
    private String senderUserId;
    private String recipientUserId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private Payment.PaymentStatus status;
    private Payment.PaymentType type;
    private LocalDateTime createdAt;
}
