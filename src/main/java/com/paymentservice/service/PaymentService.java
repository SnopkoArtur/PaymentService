package com.paymentservice.service;

import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentRequestDto dto);

    List<Payment> searchPayments(Long userId, Long orderId, String status);

    BigDecimal getTotalSum(Long userId, LocalDateTime from, LocalDateTime to);
}
