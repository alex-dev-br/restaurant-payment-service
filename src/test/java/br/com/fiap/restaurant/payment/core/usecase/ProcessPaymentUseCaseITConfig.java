package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentFinalizationGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.observability.adapter.NoOpPaymentObservabilityAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@TestConfiguration
public class ProcessPaymentUseCaseITConfig {

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway,
            ExternalPaymentProcessorGateway externalPaymentProcessorGateway,
            PaymentFinalizationGateway paymentFinalizationGateway,
            PaymentObservabilityGateway paymentObservabilityGateway
    ) {
        return new ProcessPaymentUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentFinalizationGateway,
                paymentObservabilityGateway,
                Duration.ofSeconds(30)
        );
    }

    @Bean
    public PaymentObservabilityGateway paymentObservabilityGateway() {
        return new NoOpPaymentObservabilityAdapter();
    }

    @Bean
    public RabbitProperties rabbitProperties() {
        RabbitProperties properties = new RabbitProperties();

        RabbitProperties.Exchange exchange = new RabbitProperties.Exchange();
        exchange.setOrder("ex.order");
        exchange.setPayment("ex.payment");
        properties.setExchange(exchange);

        RabbitProperties.RoutingKey routingKey = new RabbitProperties.RoutingKey();
        routingKey.setOrderCreated("order.created");
        routingKey.setPaymentApproved("payment.approved");
        routingKey.setPaymentPending("payment.pending");
        routingKey.setPaymentFailed("payment.failed");
        properties.setRoutingKey(routingKey);

        properties.setQueue(new RabbitProperties.QueueConfig());
        properties.setDlq(new RabbitProperties.Dlq());

        return properties;
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }

}
