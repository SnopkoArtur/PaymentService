package com.paymentservice.controller;

import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.entity.Payment;
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

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> create(@Valid @RequestBody PaymentRequestDto requestDto) {
        return new ResponseEntity<>(paymentService.createPayment(requestDto), HttpStatus.CREATED);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Payment> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status) {
        return paymentService.searchPayments(userId, orderId, status);
    }

    @GetMapping("/total")
    public BigDecimal getMyTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return paymentService.getTotalSum(currentUserId, from, to);
    }

    @GetMapping("/admin/total")
    @PreAuthorize("hasRole('ADMIN')")
    public BigDecimal getAllTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return paymentService.getTotalSum(null, from, to);
    }
}