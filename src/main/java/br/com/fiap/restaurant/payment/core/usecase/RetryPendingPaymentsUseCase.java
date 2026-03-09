package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;

import java.util.List;

public class RetryPendingPaymentsUseCase {

    private final PaymentRepositoryGateway paymentRepositoryGateway;
    private final ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private final PaymentEventPublisherGateway paymentEventPublisherGateway;
    private final PaymentObservabilityGateway paymentObservabilityGateway;

    public RetryPendingPaymentsUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway,
            ExternalPaymentProcessorGateway externalPaymentProcessorGateway,
            PaymentEventPublisherGateway paymentEventPublisherGateway,
            PaymentObservabilityGateway paymentObservabilityGateway
    ) {
        this.paymentRepositoryGateway = paymentRepositoryGateway;
        this.externalPaymentProcessorGateway = externalPaymentProcessorGateway;
        this.paymentEventPublisherGateway = paymentEventPublisherGateway;
        this.paymentObservabilityGateway = paymentObservabilityGateway;
    }

    public void execute() {
        List<Payment> pendingPayments = paymentRepositoryGateway.findByStatus(PaymentStatus.PENDING);

        for (Payment payment : pendingPayments) {
            retry(payment);
        }
    }

    private void retry(Payment payment) {
        try {
            paymentObservabilityGateway.logExternalProcessingStarted(payment);

            boolean approved = externalPaymentProcessorGateway.process(
                    payment.getOrderId(),
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

            Payment savedPayment = paymentRepositoryGateway.save(payment);
            paymentObservabilityGateway.logPending(savedPayment);
            paymentEventPublisherGateway.publishPending(savedPayment);

        } catch (Exception exception) {
            paymentObservabilityGateway.logExternalError(payment, exception);

            Payment savedPayment = paymentRepositoryGateway.save(payment);
            paymentObservabilityGateway.logPending(savedPayment);
            paymentEventPublisherGateway.publishPending(savedPayment);
        }
    }
}
