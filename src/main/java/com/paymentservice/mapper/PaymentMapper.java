package com.paymentservice.mapper;

import com.paymentservice.dto.PaymentRequestDto;
import com.paymentservice.dto.PaymentResponseDto;
import com.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toEntity(PaymentRequestDto dto);

    List<PaymentResponseDto> toDtoList(List<Payment> entities);

    PaymentResponseDto toDto(Payment entity);
}