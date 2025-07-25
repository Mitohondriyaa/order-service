package io.github.mitohondriyaa.order.dto;

import io.github.mitohondriyaa.order.model.UserDetails;

import java.math.BigDecimal;

public record OrderRequest(
    String productId,
    BigDecimal price,
    Integer quantity,
    UserDetails userDetails
) {}