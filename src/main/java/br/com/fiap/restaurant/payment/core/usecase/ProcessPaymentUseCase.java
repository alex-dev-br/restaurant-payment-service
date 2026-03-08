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
                .orElseGet(() -> createAndProcessPayment(orderId, clientId, amount));
    }

    private Payment createAndProcessPayment(UUID orderId, UUID clientId, BigDecimal amount) {
        Payment payment = Payment.createPending(orderId, clientId, amount);

        try {
            boolean approved = externalPaymentProcessorGateway.process(
                    payment.getId(),
                    payment.getClientId(),
                    payment.getAmount()
            );

            if (approved) {
                payment.approve();
                Payment savedPayment = paymentRepositoryGateway.save(payment);
                paymentEventPublisherGateway.publishApproved(savedPayment);
                return savedPayment;
            }

            payment.markAsPending();
            Payment savedPayment = paymentRepositoryGateway.save(payment);
            paymentEventPublisherGateway.publishPending(savedPayment);
            return savedPayment;

        } catch (Exception exception) {
            payment.markAsPending();
            Payment savedPayment = paymentRepositoryGateway.save(payment);
            paymentEventPublisherGateway.publishPending(savedPayment);
            return savedPayment;
        }
    }
}