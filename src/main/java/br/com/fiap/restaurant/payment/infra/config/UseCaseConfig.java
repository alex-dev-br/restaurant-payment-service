package br.com.fiap.restaurant.payment.infra.config;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.FindPaymentByOrderIdUseCase;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.core.usecase.RetryPendingPaymentsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

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
    public FindPaymentByOrderIdUseCase findPaymentByOrderIdUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway
    ) {
        return new FindPaymentByOrderIdUseCase(paymentRepositoryGateway);
    }

    @Bean
    public RetryPendingPaymentsUseCase retryPendingPaymentsUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway,
            ExternalPaymentProcessorGateway externalPaymentProcessorGateway,
            PaymentEventPublisherGateway paymentEventPublisherGateway,
            PaymentObservabilityGateway paymentObservabilityGateway
    ) {
        return new RetryPendingPaymentsUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway,
                paymentObservabilityGateway
        );
    }
}
