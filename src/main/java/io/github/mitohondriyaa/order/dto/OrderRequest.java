package io.github.mitohondriyaa.order.dto;

public record OrderRequest(
    String productId,
    Integer quantity
) {}