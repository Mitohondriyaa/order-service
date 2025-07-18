package io.github.mitohondriyaa.order.service;

import io.github.mitohondriyaa.order.client.InventoryClient;
import io.github.mitohondriyaa.order.dto.OrderRequest;
import io.github.mitohondriyaa.order.dto.OrderResponse;
import io.github.mitohondriyaa.order.exception.OutOfStockException;
import io.github.mitohondriyaa.order.model.Order;
import io.github.mitohondriyaa.order.model.UserDetails;
import io.github.mitohondriyaa.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderResponse placeOrder(OrderRequest orderRequest) {
        if (inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity())) {
            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setSkuCode(orderRequest.skuCode());
            order.setPrice(orderRequest.price());
            order.setQuantity(orderRequest.quantity());
            order.setEmail(orderRequest.userDetails().email());
            order.setFirstName(orderRequest.userDetails().firstName());
            order.setLastName(orderRequest.userDetails().lastName());

            orderRepository.save(order);

            return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSkuCode(),
                order.getPrice(),
                order.getQuantity(),
                new UserDetails(order.getEmail(),
                    order.getFirstName(),
                    order.getLastName())
            );
        }
        else {
            throw new OutOfStockException("Out of stock");
        }
    }
}