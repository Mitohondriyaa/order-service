package io.github.mitohondriyaa.order.client;

import io.github.mitohondriyaa.order.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.math.BigDecimal;

public interface ProductClient {
    Logger log = LoggerFactory.getLogger(ProductClient.class);

    @GetExchange("/api/product/price/{id}")
    @CircuitBreaker(name = "productServiceCircuitBreaker", fallbackMethod = "fallbackMethod")
    BigDecimal getProductPriceById(@PathVariable String id);

    default BigDecimal fallbackMethod(String id, Throwable throwable) {
        log.info("Cannot invoke product service for productId {}, failure reason: {}", id, throwable.getMessage());
        throw new ServiceUnavailableException("Service unavailable, please try again later");
    }
}