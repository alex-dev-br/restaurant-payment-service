package br.com.fiap.restaurant.payment.infra.config;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.gateway.ProcessedMessageRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.FindPaymentByOrderIdUseCase;
import br.com.fiap.restaurant.payment.core.usecase.HandleOrderCreatedEventUseCase;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.core.usecase.RetryPendingPaymentsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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
    public HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase(
            ProcessPaymentUseCase processPaymentUseCase,
            ProcessedMessageRepositoryGateway processedMessageRepositoryGateway
    ) {
        return new HandleOrderCreatedEventUseCase(
                processPaymentUseCase,
                processedMessageRepositoryGateway
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
            PaymentObservabilityGateway paymentObservabilityGateway,
            PaymentRetrySchedulerProperties paymentRetrySchedulerProperties,
            PaymentRetryPolicyProperties paymentRetryPolicyProperties
    ) {
        return new RetryPendingPaymentsUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway,
                paymentObservabilityGateway,
                Duration.ofMillis(paymentRetrySchedulerProperties.getFixedDelayMs()),
                paymentRetryPolicyProperties.getMaxAttempts(),
                paymentRetryPolicyProperties.isPublishPendingOnRetryFailure()
        );
    }
}
