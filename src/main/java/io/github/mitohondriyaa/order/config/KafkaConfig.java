package io.github.mitohondriyaa.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic orderPlacedTopic() {
        return new NewTopic("order-placed", 3, (short) 2);
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return new NewTopic("order-cancelled", 3, (short) 2);
    }
}