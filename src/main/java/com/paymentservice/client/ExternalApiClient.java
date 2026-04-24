package com.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "external-api", url = "${external-api.url}")
public interface ExternalApiClient {

    @GetMapping("/payment")
    Integer processPayment();
}