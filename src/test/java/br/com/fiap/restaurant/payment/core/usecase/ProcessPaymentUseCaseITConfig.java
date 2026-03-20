package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.infra.observability.adapter.NoOpPaymentObservabilityAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@TestConfiguration
public class ProcessPaymentUseCaseITConfig {

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway,
            ExternalPaymentProcessorGateway externalPaymentProcessorGateway,
            PaymentEventPublisherGateway paymentEventPublisherGateway,
            PaymentObservabilityGateway paymentObservabilityGateway
    ) {
        return new ProcessPaymentUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway,
                paymentObservabilityGateway,
                Duration.ofSeconds(30)
        );
    }

    @Bean
    public PaymentObservabilityGateway paymentObservabilityGateway() {
        return new NoOpPaymentObservabilityAdapter();
    }
}
