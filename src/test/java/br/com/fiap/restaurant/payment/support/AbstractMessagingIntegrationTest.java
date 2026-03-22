package br.com.fiap.restaurant.payment.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

public abstract class AbstractMessagingIntegrationTest extends AbstractPostgresIntegrationTest {

    @SuppressWarnings("resource")
    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3-management");

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void registerRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }
}
