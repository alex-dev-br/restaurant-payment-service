package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentRepositoryGateway;
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
                    .map(existingPayment -> {
                        paymentObservabilityGateway.logIdempotentReuse(existingPayment);
                        return existingPayment;
                    })
                    .orElseGet(() -> createClaimAndProcessPayment(orderId, clientId, amount));
        });
    }

    private Payment createClaimAndProcessPayment(UUID orderId, UUID clientId, BigDecimal amount) {
        Payment newPayment = Payment.createPending(orderId, clientId, amount);

        Payment claimedPayment = paymentRepositoryGateway.save(newPayment);

        if (!claimedPayment.getId().equals(newPayment.getId())) {
            paymentObservabilityGateway.logConcurrentClaimReuse(claimedPayment);
            return claimedPayment;
        }

        try {
            paymentObservabilityGateway.logExternalProcessingStarted(claimedPayment);

            boolean approved = externalPaymentProcessorGateway.process(
                    claimedPayment.getId(),
                    claimedPayment.getClientId(),
                    claimedPayment.getAmount()
            );

            if (approved) {
                claimedPayment.approve();
                Payment savedPayment = paymentRepositoryGateway.save(claimedPayment);
                paymentEventPublisherGateway.publishApproved(savedPayment);
                paymentObservabilityGateway.logApproved(savedPayment);
                return savedPayment;
            }

            claimedPayment.markAsPending();
            Payment savedPayment = paymentRepositoryGateway.save(claimedPayment);
            paymentEventPublisherGateway.publishPending(savedPayment);
            paymentObservabilityGateway.logPending(savedPayment);
            return savedPayment;

        } catch (Exception exception) {
            claimedPayment.markAsPending();
            Payment savedPayment = paymentRepositoryGateway.save(claimedPayment);
            paymentEventPublisherGateway.publishPending(savedPayment);
            paymentObservabilityGateway.logExternalError(savedPayment, exception);
            return savedPayment;
        }
    }
}