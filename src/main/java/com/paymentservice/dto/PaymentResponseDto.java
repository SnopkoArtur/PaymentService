package com.paymentservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {
    private String id;
    private Long orderId;
    private Long userId;
    private String status;
    private LocalDateTime timestamp;
    private BigDecimal paymentAmount;
}