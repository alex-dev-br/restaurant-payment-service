package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.infra.observability.adapter.NoOpPaymentObservabilityAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

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
                paymentObservabilityGateway
        );
    }

    @Bean
    public PaymentObservabilityGateway paymentObservabilityGateway() {
        return new NoOpPaymentObservabilityAdapter();
    }
}
