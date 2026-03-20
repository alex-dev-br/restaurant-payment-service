package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class RetryPendingPaymentsUseCase {

    private final PaymentRepositoryGateway paymentRepositoryGateway;
    private final ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private final PaymentEventPublisherGateway paymentEventPublisherGateway;
    private final PaymentObservabilityGateway paymentObservabilityGateway;
    private final Duration retryBackoff;
    private final int maxRetryCount;
    private final boolean publishPendingOnRetryFailure;

    public RetryPendingPaymentsUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway,
            ExternalPaymentProcessorGateway externalPaymentProcessorGateway,
            PaymentEventPublisherGateway paymentEventPublisherGateway,
            PaymentObservabilityGateway paymentObservabilityGateway,
            Duration retryBackoff,
            int maxRetryCount,
            boolean publishPendingOnRetryFailure
    ) {
        this.paymentRepositoryGateway = paymentRepositoryGateway;
        this.externalPaymentProcessorGateway = externalPaymentProcessorGateway;
        this.paymentEventPublisherGateway = paymentEventPublisherGateway;
        this.paymentObservabilityGateway = paymentObservabilityGateway;
        this.retryBackoff = Objects.requireNonNull(retryBackoff, "retryBackoff é obrigatório");
        this.maxRetryCount = maxRetryCount;
        this.publishPendingOnRetryFailure = publishPendingOnRetryFailure;
    }

    public void execute() {
        List<Payment> pendingPayments =
                paymentRepositoryGateway.findRetryablePendingPayments(OffsetDateTime.now(), maxRetryCount);

        for (Payment payment : pendingPayments) {
            retry(payment);
        }
    }

    private void retry(Payment payment) {
        try {
            paymentObservabilityGateway.logExternalProcessingStarted(payment);

            boolean approved = externalPaymentProcessorGateway.process(
                    payment.getId(),
                    payment.getClientId(),
                    payment.getAmount()
            );

            if (approved) {
                payment.approve();
                Payment savedPayment = paymentRepositoryGateway.save(payment);
                paymentObservabilityGateway.logApproved(savedPayment);
                paymentEventPublisherGateway.publishApproved(savedPayment);
                return;
            }

            handleRetryFailure(payment);

        } catch (Exception exception) {
            paymentObservabilityGateway.logExternalError(payment, exception);
            handleRetryFailure(payment);
        }
    }

    private void handleRetryFailure(Payment payment) {
        payment.registerRetryFailure(retryBackoff);
        Payment savedPayment = paymentRepositoryGateway.save(payment);
        paymentObservabilityGateway.logPending(savedPayment);

        if (publishPendingOnRetryFailure) {
            paymentEventPublisherGateway.publishPending(savedPayment);
        }
    }
}
