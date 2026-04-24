package com.paymentservice.service;

import com.paymentservice.client.ExternalApiClient;
import com.paymentservice.dao.PaymentRepository;
import com.paymentservice.dto.PaymentEventDto;
import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.dto.TotalAmountDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.mapper.PaymentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.bson.Document;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private ExternalApiClient externalApiClient;
    @Mock private KafkaTemplate<String, PaymentEventDto> kafkaTemplate;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createPayment_Success() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);
        request.setPaymentAmount(new BigDecimal("100"));

        Payment payment = new Payment();
        payment.setOrderId(1L);

        when(paymentMapper.toEntity(any())).thenReturn(payment);
        when(externalApiClient.processPayment()).thenReturn(2);
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentMapper.toDto(any())).thenReturn(new PaymentResponseDto());

        paymentService.createPayment(request);

        assertEquals("SUCCESS", payment.getStatus());
        verify(kafkaTemplate).send(eq("payment-events"), any(PaymentEventDto.class));
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPayment_Failed() {
        Payment payment = new Payment();
        when(paymentMapper.toEntity(any())).thenReturn(payment);
        when(externalApiClient.processPayment()).thenReturn(1);
        when(paymentRepository.save(any())).thenReturn(payment);

        paymentService.createPayment(new PaymentRequestDto());

        assertEquals("FAILED", payment.getStatus());
    }


    @Test
    void searchPayments_ByOrderId() {
        when(paymentRepository.findAllByOrderId(1L)).thenReturn(List.of(new Payment()));

        List<Payment> result = paymentService.searchPayments(null, 1L, null);

        assertEquals(1, result.size());
        verify(paymentRepository).findAllByOrderId(1L);
    }

    @Test
    void searchPayments_ByUserId() {
        when(paymentRepository.findAllByUserId(1L)).thenReturn(List.of(new Payment()));

        List<Payment> result = paymentService.searchPayments(1L, null, null);

        assertEquals(1, result.size());
        verify(paymentRepository).findAllByUserId(1L);
    }

    @Test
    void searchPayments_All() {
        when(paymentRepository.findAll()).thenReturn(List.of(new Payment(), new Payment()));

        List<Payment> result = paymentService.searchPayments(null, null, null);

        assertEquals(2, result.size());
        verify(paymentRepository).findAll();
    }

    @Test
    void getTotalSum_ShouldReturnCorrectValue() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        TotalAmountDto mockResult = new TotalAmountDto();
        mockResult.setTotal(new BigDecimal("550.50"));

        AggregationResults<TotalAmountDto> aggregationResults =
                new AggregationResults<>(List.of(mockResult), new Document());

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(TotalAmountDto.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentService.getTotalSum(null, from, to);

        assertNotNull(result);
        assertEquals(new BigDecimal("550.50"), result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("payments"), eq(TotalAmountDto.class));
    }

    @Test
    void getTotalSum_ShouldReturnZeroWhenNoData() {
        AggregationResults<TotalAmountDto> emptyResults =
                new AggregationResults<>(Collections.emptyList(), new Document());

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(TotalAmountDto.class)))
                .thenReturn(emptyResults);

        BigDecimal result = paymentService.getTotalSum(1L, LocalDateTime.now(), LocalDateTime.now());

        assertEquals(BigDecimal.ZERO, result);
    }
}