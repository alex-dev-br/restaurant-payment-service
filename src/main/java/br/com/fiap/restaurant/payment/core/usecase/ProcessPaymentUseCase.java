package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public class ProcessPaymentUseCase {

    private final PaymentRepositoryGateway paymentRepositoryGateway;
    private final ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private final PaymentEventPublisherGateway paymentEventPublisherGateway;

    public ProcessPaymentUseCase(
            PaymentRepositoryGateway paymentRepositoryGateway,
            ExternalPaymentProcessorGateway externalPaymentProcessorGateway,
            PaymentEventPublisherGateway paymentEventPublisherGateway
    ) {
        this.paymentRepositoryGateway = paymentRepositoryGateway;
        this.externalPaymentProcessorGateway = externalPaymentProcessorGateway;
        this.paymentEventPublisherGateway = paymentEventPublisherGateway;
    }

    public Payment execute(UUID orderId, UUID clientId, BigDecimal amount) {
        return paymentRepositoryGateway.findByOrderId(orderId)
                .orElseGet(() -> createClaimAndProcessPayment(orderId, clientId, amount));
    }

    private Payment createClaimAndProcessPayment(UUID orderId, UUID clientId, BigDecimal amount) {
        Payment newPayment = Payment.createPending(orderId, clientId, amount);

        Payment claimedPayment = paymentRepositoryGateway.save(newPayment);

        if (!claimedPayment.getId().equals(newPayment.getId())) {
            return claimedPayment;
        }

        try {
            boolean approved = externalPaymentProcessorGateway.process(
                    claimedPayment.getId(),
                    claimedPayment.getClientId(),
                    claimedPayment.getAmount()
            );

            if (approved) {
                claimedPayment.approve();
                Payment savedPayment = paymentRepositoryGateway.save(claimedPayment);
                paymentEventPublisherGateway.publishApproved(savedPayment);
                return savedPayment;
            }

            claimedPayment.markAsPending();
            Payment savedPayment = paymentRepositoryGateway.save(claimedPayment);
            paymentEventPublisherGateway.publishPending(savedPayment);
            return savedPayment;

        } catch (Exception exception) {
            claimedPayment.markAsPending();
            Payment savedPayment = paymentRepositoryGateway.save(claimedPayment);
            paymentEventPublisherGateway.publishPending(savedPayment);
            return savedPayment;
        }
    }
}