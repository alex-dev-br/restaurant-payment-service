package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaMessagingConfig {
}
