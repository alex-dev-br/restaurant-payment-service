package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;

public class ProcessPaymentUseCase {

    private final PaymentRepositoryGateway paymentRepositoryGateway;
    private final ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private final PaymentEventPublisherGateway paymentEventPublisherGateway;
    private final PaymentObservabilityGateway paymentObservabilityGateway;

    public ProcessPaymentUseCase(
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

    public Payment execute(ProcessPaymentCommand command) {
        return paymentObservabilityGateway.measureProcessing(() -> {
            paymentObservabilityGateway.logProcessingStarted(
                    command.orderId(),
                    command.clientId(),
                    command.amount()
            );

            return paymentRepositoryGateway.findByOrderId(command.orderId())
                    .map(this::reuseExistingPayment)
                    .orElseGet(() -> claimAndProcessPayment(command));
        });
    }

    private Payment reuseExistingPayment(Payment existingPayment) {
        paymentObservabilityGateway.logIdempotentReuse(existingPayment);
        return existingPayment;
    }

    private Payment claimAndProcessPayment(ProcessPaymentCommand command) {
        Payment newPayment = Payment.createPending(
                command.orderId(),
                command.clientId(),
                command.amount()
        );

        Payment claimedPayment = paymentRepositoryGateway.save(newPayment);

        if (isConcurrentClaimReuse(newPayment, claimedPayment)) {
            paymentObservabilityGateway.logConcurrentClaimReuse(claimedPayment);
            return claimedPayment;
        }

        return processClaimedPayment(claimedPayment);
    }

    private boolean isConcurrentClaimReuse(Payment newPayment, Payment claimedPayment) {
        return !claimedPayment.getId().equals(newPayment.getId());
    }

    private Payment processClaimedPayment(Payment payment) {
        try {
            paymentObservabilityGateway.logExternalProcessingStarted(payment);

            boolean approved = externalPaymentProcessorGateway.process(
                    payment.getId(),
                    payment.getClientId(),
                    payment.getAmount()
            );

            if (approved) {
                payment.approve();
                return persistAndPublishApproved(payment);
            }

            payment.markAsPending();
            return persistAndPublishPending(payment);

        } catch (Exception exception) {
            payment.markAsPending();
            paymentObservabilityGateway.logExternalError(payment, exception);
            return persistAndPublishPending(payment);
        }
    }

    private Payment persistAndPublishApproved(Payment payment) {
        Payment savedPayment = paymentRepositoryGateway.save(payment);
        paymentEventPublisherGateway.publishApproved(savedPayment);
        paymentObservabilityGateway.logApproved(savedPayment);
        return savedPayment;
    }

    private Payment persistAndPublishPending(Payment payment) {
        Payment savedPayment = paymentRepositoryGateway.save(payment);
        paymentEventPublisherGateway.publishPending(savedPayment);
        paymentObservabilityGateway.logPending(savedPayment);
        return savedPayment;
    }
}
