package com.payments.gateway.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${payment.kafka.topic.initiated}")
    private String topicName;

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 3;

    @Bean
    public NewTopic paymentInitiatedTopic() {
        return TopicBuilder.name(topicName)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic paymentAuthorizedTopic() {
        return TopicBuilder.name("payment_authorized")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment_failed")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic paymentClearedTopic() {
        return TopicBuilder.name("payment_cleared")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic paymentSettledTopic() {
        return TopicBuilder.name("payment_settled")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}
