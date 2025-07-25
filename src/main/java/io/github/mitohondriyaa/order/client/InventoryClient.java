package io.github.mitohondriyaa.order.client;

import io.github.mitohondriyaa.order.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface InventoryClient {
    Logger log = LoggerFactory.getLogger(InventoryClient.class);

    @GetExchange("/api/inventory")
    @CircuitBreaker(name = "inventoryServiceCircuitBreaker", fallbackMethod = "fallbackMethod")
    boolean isInStock(@RequestParam String productId, @RequestParam Integer quantity);

    default boolean fallbackMethod(String skuCode, Integer quantity, Throwable throwable) {
        log.info("Cannot invoke inventory service for skuCode {}, failure reason: {}", skuCode, throwable.getMessage());
        throw new ServiceUnavailableException("Service unavailable, please try again later");
    }
}