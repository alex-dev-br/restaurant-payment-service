package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentFinalizationGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;

import java.time.Duration;
import java.util.Objects;

public class ProcessPaymentUseCase {

    private final PaymentRepositoryGateway paymentRepositoryGateway;
    private final ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private final PaymentFinalizationGateway paymentFinalizationGateway;
    private final PaymentObservabilityGateway paymentObservabilityGateway;
    private final Duration retryBackoff;

    public ProcessPaymentUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway,
            ExternalPaymentProcessorGateway externalPaymentProcessorGateway,
            PaymentFinalizationGateway paymentFinalizationGateway,
            PaymentObservabilityGateway paymentObservabilityGateway,
            Duration retryBackoff
    ) {
        this.paymentRepositoryGateway = paymentRepositoryGateway;
        this.externalPaymentProcessorGateway = externalPaymentProcessorGateway;
        this.paymentFinalizationGateway = paymentFinalizationGateway;
        this.paymentObservabilityGateway = paymentObservabilityGateway;
        this.retryBackoff = Objects.requireNonNull(retryBackoff, "retryBackoff é obrigatório");
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
                return persistAndEnqueueApproved(payment);
            }

            payment.registerRetryFailure(retryBackoff);
            return persistAndEnqueuePending(payment);

        } catch (Exception exception) {
            paymentObservabilityGateway.logExternalError(payment, exception);
            payment.registerRetryFailure(retryBackoff);
            return persistAndEnqueuePending(payment);
        }
    }

    private Payment persistAndEnqueueApproved(Payment payment) {
        Payment savedPayment = paymentFinalizationGateway.saveApprovedAndEnqueue(payment);
        paymentObservabilityGateway.logApproved(savedPayment);
        return savedPayment;
    }

    private Payment persistAndEnqueuePending(Payment payment) {
        Payment savedPayment = paymentFinalizationGateway.savePendingAndEnqueue(payment);
        paymentObservabilityGateway.logPending(savedPayment);
        return savedPayment;
    }
}
