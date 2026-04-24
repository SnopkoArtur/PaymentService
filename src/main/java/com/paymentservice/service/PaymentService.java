package com.paymentservice.service;

import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for processing and managing payments
 */
public interface PaymentService {
    /**
     * Creates a new payment
     *
     * @param dto the data required to create a payment
     * @return created payment data
     */
    PaymentResponseDto createPayment(PaymentRequestDto dto);

    /**
     * Filters payment records by user, order, or status.
     *
     * @param userId  user id
     * @param orderId order id
     * @param status  status string
     * @return filtered payments
     */
    List<Payment> searchPayments(Long userId, Long orderId, String status);

    /**
     * Aggregates the total sum
     *
     * @param userId (optional) user id
     * @param from   the beginning of the period.
     * @param to     the end of the period.
     * @return total sum
     */
    BigDecimal getTotalSum(Long userId, LocalDateTime from, LocalDateTime to);
}
