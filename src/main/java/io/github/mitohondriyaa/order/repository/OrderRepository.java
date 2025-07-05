package io.github.mitohondriyaa.order.repository;

import io.github.mitohondriyaa.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

}