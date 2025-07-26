package io.github.mitohondriyaa.order.dto;

import java.math.BigDecimal;

public record OrderRequest(
    String productId,
    BigDecimal price,
    Integer quantity
) {}