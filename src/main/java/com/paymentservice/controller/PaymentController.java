package com.paymentservice.controller;

import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing payment operations.
 * Provides endpoints for creating payments, searching records, and calculating financial totals.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a new payment process.
     *
     * @param requestDto details of the payment
     * @return created payment
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> create(@Valid @RequestBody PaymentRequestDto requestDto) {
        return new ResponseEntity<>(paymentService.createPayment(requestDto), HttpStatus.CREATED);
    }

    /**
     * Gets payments depending on gives arguments
     *
     * @param userId  user id
     * @param orderId order id
*    * @param status  status
     * @return list of payments
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status) {
        List<PaymentResponseDto> result = paymentService.searchPayments(userId, orderId, status);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns total with given time limits for user
     *
     * @param from start date
     * @param to   end date
     * @return total value
     */
    @GetMapping("/total")
    public BigDecimal getMyTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return paymentService.getTotalSum(currentUserId, from, to);
    }
    /**
     * Returns total with given time limits for all payments
     *
     * @param from start date \
     * @param to   end date \
     * @return total value
     */
    @GetMapping("/admin/total")
    @PreAuthorize("hasRole('ADMIN')")
    public BigDecimal getAllTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return paymentService.getTotalSum(null, from, to);
    }
}