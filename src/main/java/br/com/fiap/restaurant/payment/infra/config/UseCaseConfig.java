package br.com.fiap.restaurant.payment.infra.config;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventTransportGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentFinalizationGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentOutboxRepositoryGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.gateway.ProcessedMessageRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.FindPaymentByOrderIdUseCase;
import br.com.fiap.restaurant.payment.core.usecase.HandleOrderCreatedEventUseCase;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.core.usecase.PublishPendingPaymentOutboxUseCase;
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
            PaymentFinalizationGateway paymentFinalizationGateway,
            PaymentObservabilityGateway paymentObservabilityGateway,
            PaymentRetrySchedulerProperties paymentRetrySchedulerProperties
    ) {
        return new ProcessPaymentUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentFinalizationGateway,
                paymentObservabilityGateway,
                Duration.ofMillis(paymentRetrySchedulerProperties.getFixedDelayMs())
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
            PaymentFinalizationGateway paymentFinalizationGateway,
            PaymentObservabilityGateway paymentObservabilityGateway,
            PaymentRetrySchedulerProperties paymentRetrySchedulerProperties,
            PaymentRetryPolicyProperties paymentRetryPolicyProperties
    ) {
        return new RetryPendingPaymentsUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentFinalizationGateway,
                paymentObservabilityGateway,
                Duration.ofMillis(paymentRetrySchedulerProperties.getFixedDelayMs()),
                paymentRetryPolicyProperties.getMaxAttempts(),
                paymentRetryPolicyProperties.isPublishPendingOnRetryFailure()
        );
    }

    @Bean
    public PublishPendingPaymentOutboxUseCase publishPendingPaymentOutboxUseCase(
            PaymentOutboxRepositoryGateway paymentOutboxRepositoryGateway,
            PaymentEventTransportGateway paymentEventTransportGateway,
            PaymentOutboxPublisherProperties paymentOutboxPublisherProperties
    ) {
        return new PublishPendingPaymentOutboxUseCase(
                paymentOutboxRepositoryGateway,
                paymentEventTransportGateway,
                paymentOutboxPublisherProperties.getBatchSize()
        );
    }
}
