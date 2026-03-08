package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

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

    public Payment execute(UUID orderId, UUID clientId, BigDecimal amount) {
        return paymentObservabilityGateway.measureProcessing(() -> {
            paymentObservabilityGateway.logProcessingStarted(orderId, clientId, amount);

            return paymentRepositoryGateway.findByOrderId(orderId)
                    .map(this::reuseExistingPayment)
                    .orElseGet(() -> claimAndProcessPayment(orderId, clientId, amount));
        });
    }

    private Payment reuseExistingPayment(Payment existingPayment) {
        paymentObservabilityGateway.logIdempotentReuse(existingPayment);
        return existingPayment;
    }

    private Payment claimAndProcessPayment(UUID orderId, UUID clientId, BigDecimal amount) {
        Payment newPayment = Payment.createPending(orderId, clientId, amount);
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
                return approvePayment(payment);
            }

            return keepPaymentPending(payment);

        } catch (Exception exception) {
            return handleProcessingError(payment, exception);
        }
    }

    private Payment approvePayment(Payment payment) {
        payment.approve();

        Payment savedPayment = paymentRepositoryGateway.save(payment);
        paymentEventPublisherGateway.publishApproved(savedPayment);
        paymentObservabilityGateway.logApproved(savedPayment);

        return savedPayment;
    }

    private Payment keepPaymentPending(Payment payment) {
        payment.markAsPending();

        Payment savedPayment = paymentRepositoryGateway.save(payment);
        paymentEventPublisherGateway.publishPending(savedPayment);
        paymentObservabilityGateway.logPending(savedPayment);

        return savedPayment;
    }

    private Payment handleProcessingError(Payment payment, Exception exception) {
        payment.markAsPending();

        Payment savedPayment = paymentRepositoryGateway.save(payment);
        paymentEventPublisherGateway.publishPending(savedPayment);
        paymentObservabilityGateway.logExternalError(savedPayment, exception);
        paymentObservabilityGateway.logPending(savedPayment);

        return savedPayment;
    }
}