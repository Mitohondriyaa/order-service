package io.github.mitohondriyaa.order.controller;

import io.github.mitohondriyaa.order.dto.OrderRequest;
import io.github.mitohondriyaa.order.dto.OrderResponse;
import io.github.mitohondriyaa.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
        @RequestBody OrderRequest orderRequest,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.placeOrder(orderRequest, jwt);
    }
}