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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
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
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEventDto.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        paymentService.createPayment(request);

        assertEquals("SUCCESS", payment.getStatus());
        verify(kafkaTemplate).send(eq("payment-events"), any(), any(PaymentEventDto.class));
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPayment_Failed() {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        when(paymentMapper.toEntity(any())).thenReturn(payment);
        when(externalApiClient.processPayment()).thenReturn(1);
        when(paymentRepository.save(any())).thenReturn(payment);
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEventDto.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        paymentService.createPayment(new PaymentRequestDto());

        assertEquals("FAILED", payment.getStatus());
    }


    @Test
    void searchPayments_ByOrderId() {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        Payment payment2 = new Payment();
        payment.setOrderId(2L);
        List<Payment> payments = List.of(payment, payment2);
        PaymentResponseDto dto = new PaymentResponseDto();

        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);
        when(paymentMapper.toDtoList(payments)).thenReturn(List.of(dto));

        List<PaymentResponseDto> result = paymentService.searchPayments(null, 1L, null);

        assertEquals(1, result.size());
        verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
        verify(paymentRepository, never()).findAllByOrderId(anyLong());
    }

    @Test
    void searchPayments_ByUserId() {
        Payment payment = new Payment();
        payment.setUserId(1L);
        Payment payment2 = new Payment();
        payment.setUserId(2L);
        List<Payment> payments = List.of(payment, payment2);

        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);
        when(paymentMapper.toDtoList(any())).thenReturn(List.of(new PaymentResponseDto()));

        List<PaymentResponseDto> result = paymentService.searchPayments(2L, null, null);
        assertEquals(1, result.size());
        verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
    }

    @Test
    void searchPayments_All() {
        List<Payment> payments = List.of(new Payment(), new Payment());

        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);
        when(paymentMapper.toDtoList(any())).thenReturn(List.of(new PaymentResponseDto(), new PaymentResponseDto()));

        List<PaymentResponseDto> result = paymentService.searchPayments(null, null, null);

        assertEquals(2, result.size());
        verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
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

    @Test
     void createPayment_BankError_ThrowsException() {
        when(paymentMapper.toEntity(any())).thenReturn(new Payment());
        when(externalApiClient.processPayment()).thenThrow(feign.FeignException.class);
        assertThrows(feign.FeignException.class, () -> paymentService.createPayment(new PaymentRequestDto()));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_KafkaError_ShouldStillSucceed() {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setStatus("SUCCESS");

        when(paymentMapper.toEntity(any())).thenReturn(payment);
        when(externalApiClient.processPayment()).thenReturn(2);
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentMapper.toDto(any())).thenReturn(new PaymentResponseDto());

        CompletableFuture<SendResult<String, PaymentEventDto>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka connection lost"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        assertDoesNotThrow(() -> paymentService.createPayment(new PaymentRequestDto()));

        verify(paymentRepository).save(any());
        assertTrue(future.isCompletedExceptionally());
    }
}