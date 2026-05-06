package com.paymentservice.service;

import com.paymentservice.client.ExternalApiClient;
import com.paymentservice.dao.PaymentRepository;
import com.paymentservice.dto.PaymentEventDto;
import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.dto.TotalAmountDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.mapper.PaymentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final MongoTemplate mongoTemplate;
    private final PaymentMapper paymentMapper;
    private final ExternalApiClient externalApiClient;
    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

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

        PaymentEventDto event = PaymentEventDto.builder()
                .eventType("CREATE_PAYMENT")
                .orderId(savedPayment.getOrderId())
                .status(savedPayment.getStatus())
                .build();

        sendEventWithCallback(event);

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public List<PaymentResponseDto> searchPayments(Long userId, Long orderId, String status) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        if (userId != null) {
            criteria.add(Criteria.where("user_id").is(userId));
        }

        if (orderId != null) {
            criteria.add(Criteria.where("order_id").is(orderId));
        }

        if (status != null && !status.isBlank()) {
            criteria.add(Criteria.where("status").is(status));
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        List<Payment> payments = mongoTemplate.find(query, Payment.class);

        return paymentMapper.toDtoList(payments);
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

    private void sendEventWithCallback(PaymentEventDto event) {
        String key = event.getOrderId().toString();

        CompletableFuture<SendResult<String, PaymentEventDto>> future =
                kafkaTemplate.send("payment-events", key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent event [{}], orderId: {}, partition: {}",
                        event.getEventType(), key, result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send event [{}] for orderId: {}. Error: {}",
                        event.getEventType(), key, ex.getMessage());
            }
        });
    }

}