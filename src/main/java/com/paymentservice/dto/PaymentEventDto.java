package com.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEventDto implements Serializable {

    private String eventType;

    @Builder.Default
    private String version = "1.0";

    private Long orderId;
    private String status;
}