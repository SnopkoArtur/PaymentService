package com.paymentservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TotalAmountDto {
    private BigDecimal total;
}