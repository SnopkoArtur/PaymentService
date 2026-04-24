package com.paymentservice.service;

import com.paymentservice.client.ExternalApiClient;
import com.paymentservice.dao.PaymentRepository;
import com.paymentservice.dto.PaymentEventDto;
import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.dto.TotalAmountDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final MongoTemplate mongoTemplate;
    private final PaymentMapper paymentMapper;
    private final ExternalApiClient externalApiClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto dto) {
        Payment payment = paymentMapper.toEntity(dto);
        payment.setTimestamp(LocalDateTime.now());

        Integer randomNumber = externalApiClient.processPayment();

        if (randomNumber % 2 == 0) {
            payment.setStatus("SUCCESS");
        } else {
            payment.setStatus("FAILED");
        }
        Payment savedPayment = paymentRepository.save(payment);

        PaymentEventDto event = new PaymentEventDto(savedPayment.getOrderId(), savedPayment.getStatus());
        kafkaTemplate.send("payment-events", event);

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public List<Payment> searchPayments(Long userId, Long orderId, String status) {
        if (orderId != null) return paymentRepository.findAllByOrderId(orderId);
        if (userId != null) return paymentRepository.findAllByUserId(userId);
        if (status != null) return paymentRepository.findAllByStatus(status);
        return paymentRepository.findAll();
    }

    @Override
    public BigDecimal getTotalSum(Long userId, LocalDateTime from, LocalDateTime to) {
        Criteria criteria = Criteria.where("timestamp").gte(from).lte(to);

        if (userId != null) {
            criteria.and("user_id").is(userId);
        }

        criteria.and("status").is("SUCCESS");

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum("payment_amount").as("total")
        );

        AggregationResults<TotalAmountDto> results = mongoTemplate.aggregate(
                aggregation, "payments", TotalAmountDto.class
        );

        return results.getUniqueMappedResult() != null
                ? results.getUniqueMappedResult().getTotal()
                : BigDecimal.ZERO;
    }
}