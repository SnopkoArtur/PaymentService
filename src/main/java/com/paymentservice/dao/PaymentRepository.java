package com.paymentservice.dao;

import com.paymentservice.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findAllByUserId(Long userId);
    List<Payment> findAllByOrderId(Long orderId);
    List<Payment> findAllByStatus(String status);
}